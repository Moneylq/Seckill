package top.idalin.service.impl;

import org.apache.commons.collections.MapUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import top.idalin.dao.SeckillDao;
import top.idalin.dao.SuccessKilledDao;
import top.idalin.dao.cache.RedisDao;
import top.idalin.dto.Exposer;
import top.idalin.dto.SeckillExecution;
import top.idalin.entity.Seckill;
import top.idalin.entity.SuccessKilled;
import top.idalin.enums.SeckillStateEnum;
import top.idalin.exception.RepeatKillException;
import top.idalin.exception.SeckillCloseException;
import top.idalin.exception.SeckillException;
import top.idalin.service.SeckillService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SeckillServiceImpl implements SeckillService {

    private org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());
    // 注入Service的依赖
    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private SuccessKilledDao successKilledDao;

    @Autowired
    private RedisDao redisDao;

    // MD5盐值字符串，用于混淆MD5
    private final String slat = "fejKofjIfwY*Y&%&^09";

    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0,4);
    }

    public Seckill getById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    public Exposer exportSeckillUrl(long seckillId) {

        // 优化点： 缓存优化 : 一致性的维护，建立在超时的基础上
        /**
         * get data from redis
         * if null
         * get db
         * else
         *      put cache
         * locgoin
         */
        // 1: 访问Redis
        Seckill seckill = redisDao.getSeckill(seckillId);

        if (seckill == null) {
            // 2. 如果缓存中没有，则去访问数据库
            seckill = seckillDao.queryById(seckillId);

            if(seckill==null) {
                // 如果数据库中也没有，则直接返回一个false,代表秒杀单不存在
                return new Exposer(false,seckillId);
            } else {
                // 存在的情况， 将对象放入redis
                redisDao.putSeckill(seckill);
            }
        }

        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        // 系统当前时间
        Date nowTime = new Date();
        if(nowTime.getTime() < startTime.getTime() || nowTime.getTime() > endTime.getTime()) {
            return new Exposer(false,seckillId,nowTime.getTime(),startTime.getTime(),endTime.getTime());
        }
        String md5 = getMD5(seckillId);
        return new Exposer(true,md5,seckillId);
    }

    private String getMD5(long seckillId) {
        String base = seckillId + "/" + slat;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

    /**
     * 使用注解控制事务方法的优点：
     * 1、开发团队达成一致约定，明确标注事务方法的编程风格
     * 2、保证事务方法的执行时间尽可能短，不要穿插其它网络操作RPC/HTTP请求或者剥离到事务方法外部
     * 3、不是所有的方法都需要事务，如： 只有一条修改操作，只读操作不需要事务控制.
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    @Transactional
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || !md5.equals(getMD5(seckillId))) {
            throw new SeckillException("seckill data rewrite");
        }
        // 执行秒杀逻辑，减库存 + 记录购买行为
        Date nowTime = new Date();
        // 优化： 调整代码的执行顺序
        try {

            // 购买成功，记录购买行为
            int insertCount = successKilledDao.insertSuccessKilled(seckillId,userPhone);
            // 唯一： seckillId,userPhone
            if(insertCount <= 0) {
                // 重复秒杀
                throw new RepeatKillException("seckill repeated!!");
            } else {

                // 减库存
                int updateCount = seckillDao.reduceNumber(seckillId,nowTime);
                if(updateCount <= 0) {
                    // 没有更新到记录，秒杀结束  rollback
                    throw new SeckillCloseException("seckill is closed");
                } else {
                    // 秒杀成功  commit
                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId,userPhone);
                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS,successKilled);
                }
            }
        } catch (SeckillCloseException e1) {
            throw e1;
        } catch (RepeatKillException e2) {
            throw e2;
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            // 所有编译器异常，转化为运行期异常
            throw new SeckillException("seckill inner error:" + e.getMessage() );
        }
    }

    @Override
    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5) {
        if (md5 == null || !md5.equals(getMD5(seckillId))) {
            return new SeckillExecution(seckillId,SeckillStateEnum.DATA_REWRITE);
        }
        Date killTime = new Date();
        Map<String ,Object> map = new HashMap<String, Object>();
        map.put("seckillId",seckillId);
        map.put("phone",userPhone);
        map.put("killTime",killTime);
        map.put("result",null);
        // 执行存储过程，result被复制
        try {
            seckillDao.killByProcedure(map);
            // 获取result
            // 引用common-collections
            int result = MapUtils.getInteger(map, "result", -2);
            if (result == 1) {
                // 秒杀成功
                SuccessKilled sk = successKilledDao.queryByIdWithSeckill(seckillId,userPhone);

                return new SeckillExecution(seckillId,SeckillStateEnum.SUCCESS,sk);
            } else {
                return new SeckillExecution(seckillId,SeckillStateEnum.stateOf(result));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new SeckillExecution(seckillId,SeckillStateEnum.INNER_ERROR);
        }
    }
}
