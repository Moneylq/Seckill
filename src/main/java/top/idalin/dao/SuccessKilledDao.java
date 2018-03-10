package top.idalin.dao;

import org.apache.ibatis.annotations.Param;
import top.idalin.entity.SuccessKilled;

public interface SuccessKilledDao {

    /**
     * 查询购买明细，可过滤重复
     * @param seckillId
     * @param userPhone
     * @return  插入的行数
     */
    int insertSuccessKilled(@Param("seckillId") long seckillId,@Param("userPhone") long userPhone);

    /**
     * 根据id去查询SuccessKilled并携带秒杀对象实体
     * @param seckillId
     * @return
     */
    SuccessKilled queryByIdWithSeckill(@Param("seckillId") long seckillId,@Param("userPhone") long userPhone);

}
