package top.idalin.service.impl;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import top.idalin.dao.SeckillDao;
import top.idalin.dao.SuccessKilledDao;
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
import java.util.List;

public class SeckillServiceImpl implements SeckillService {

    private org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private SuccessKilledDao successKilledDao;

    // MD5盐值字符串，用于混淆MD5
    private final String slat = "fjslfoiuwejfDSJKofjdJHDIUIfwY*Y&%&^09";

    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0,4);
    }

    public Seckill getById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    public Exposer exportSeckillUrl(long seckillId) {
        Seckill seckill = seckillDao.queryById(seckillId);
        if(seckill==null) {
            return new Exposer(false,seckillId);
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

    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || md5.equals(getMD5(seckillId))) {
            throw new SeckillException("seckill data rewrite");
        }
        // 执行秒杀逻辑，减库存 + 记录购买行为
        Date nowTime = new Date();
        try {
            // 减库存
            int updateCount = seckillDao.reduceNumber(seckillId,nowTime);
            if(updateCount <= 0) {
                // 没有更新到记录，秒杀结束
                throw new SeckillCloseException("seckill is closed");
            } else {
                // 购买成功，记录购买行为
                int insertCount = successKilledDao.insertSuccessKilled(seckillId,userPhone);
                // 唯一： seckillId,userPhone
                if(insertCount <= 0) {
                    // 重复秒杀
                    throw new RepeatKillException("seckill repeated!!");
                } else {
                    // 秒杀成功
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
}
