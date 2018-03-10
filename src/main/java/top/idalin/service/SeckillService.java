package top.idalin.service;

import top.idalin.dto.Exposer;
import top.idalin.dto.SeckillExecution;
import top.idalin.entity.Seckill;
import top.idalin.exception.RepeatKillException;
import top.idalin.exception.SeckillCloseException;
import top.idalin.exception.SeckillException;

import java.util.List;

/**
 * 业务接口： 站在“使用者”的角度去考虑问题
 * 三个方面：
 *      方法定义的粒度
 *      参数
 *      返回类型（return 类型/异常）
 */
public interface SeckillService {

    /**
     * 查询所有秒杀记录
     */
    List<Seckill> getSeckillList();

    /**
     * 查询单个秒杀记录
     * @param seckillId
     * @return
     */
    Seckill getById(long seckillId);

    /**
     * 秒杀开启是输出秒杀接口地址
     * 否则输出系统时间和秒杀时间
     * @param seckillId
     * @return
     */
    Exposer exportSeckillUrl(long seckillId);

    /**
     * 执行秒杀操作
     * @param seckillId
     * @param userPhone
     * @param md5
     */
    SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException,RepeatKillException,SeckillCloseException;

}
