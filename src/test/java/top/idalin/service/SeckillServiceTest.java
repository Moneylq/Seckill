package top.idalin.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import top.idalin.dto.Exposer;
import top.idalin.dto.SeckillExecution;
import top.idalin.entity.Seckill;
import top.idalin.exception.RepeatKillException;
import top.idalin.exception.SeckillCloseException;

import java.util.List;

import static org.junit.Assert.*;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({
        "classpath:spring/spring-dao.xml",
        "classpath:spring/spring-service.xml"
})
public class SeckillServiceTest {

    // 日志
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @Test
    public void getSeckillList() {

        List<Seckill> lists = seckillService.getSeckillList();
        logger.info("lists={}",lists);

    }

    @Test
    public void getById() {
        long id = 1000;
        Seckill seckill = seckillService.getById(id);
        logger.info("seckill={}",seckill);
    }

    // 集成测试代码的完整逻辑，注意可重复执行
    @Test
    public void testSeckillLogic() {
        long id = 1000;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        if (exposer.isExposed()) {
            logger.info("exposer={}", exposer);

            long phone = 15621508134L;
            String md5 = "8fcca9aaa6edbb01f818154cd30eed31";

            try {
                SeckillExecution execution = seckillService.executeSeckill(id,phone,md5);
                logger.info("result={}",execution);
            } catch (RepeatKillException e) {
                logger.error(e.getMessage());
            } catch (SeckillCloseException e2) {
                logger.error(e2.getMessage());
            }

        } else {
            // 秒杀未开启
            logger.warn("exposer={}",exposer);
        }
    }

    @Test
    public void exportSeckillUrl() {
        long id = 1001;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        logger.info("exposer={}",exposer);
        /**
         * Exposer{
         * exposed=true,
         * md5='8fcca9aaa6edbb01f818154cd30eed31',
         * seckillId=1001,
         * now=0,
         * start=0,
         * end=0}
         */
    }

    @Test
    public void executeSeckill() {
        long id = 1001;
        long phone = 15621508134L;
        String md5 = "8fcca9aaa6edbb01f818154cd30eed31";

        try {
            SeckillExecution execution = seckillService.executeSeckill(id,phone,md5);
            logger.info("result={}",execution);
        } catch (RepeatKillException e) {
            logger.error(e.getMessage());
        } catch (SeckillCloseException e2) {
            logger.error(e2.getMessage());
        }
    }
}