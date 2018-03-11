package top.idalin.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import top.idalin.dto.Exposer;
import top.idalin.dto.SeckillExecution;
import top.idalin.dto.SeckillResult;
import top.idalin.entity.Seckill;
import top.idalin.enums.SeckillStateEnum;
import top.idalin.exception.RepeatKillException;
import top.idalin.exception.SeckillCloseException;
import top.idalin.service.SeckillService;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/seckill")  // url: /模块/资源/{id}/细分   例： /seckill/list
public class SeckillController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @RequestMapping(name = "/list", method = RequestMethod.GET)
    public String list(Model model) {

        // 获取列表页
        List<Seckill> list = seckillService.getSeckillList();
        model.addAttribute("list", list);

        return "list";

    }

    @RequestMapping(name = "/{seckillId}/detail", method = RequestMethod.GET)
    public String detail(@PathVariable("seckillId") Long seckillId, Model model) {
        if (seckillId == null) {
            return "redirect:/seckill/list";
        }
        Seckill seckill = seckillService.getById(seckillId);
        if (seckill == null) {
            return "forward:/seckill/list";
        }
        model.addAttribute("seckill", seckill);

        return "detail";
    }

    @RequestMapping(value = "/{seckillId}/exposer",
            method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Exposer> exposer(Long seckillId) {
        SeckillResult<Exposer> result;

        try {
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            result = new SeckillResult<Exposer>(true, exposer);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result = new SeckillResult<Exposer>(false, e.getMessage());
        }
        return result;
    }

    @RequestMapping(value = "/{seckillId}/{md5}/execution",
            method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> excution(@PathVariable("seckillId") Long seckillId,
                                                    @PathVariable("md5") String md5,
                                                    @CookieValue(value = "killPhone", required = false) Long phone) {
        // 也可以使用SpringMVC的 Valid 来校验
        if (phone == null) {
            return new SeckillResult<SeckillExecution>(false,"未注册！！");
        }
        SeckillResult<SeckillExecution> result;
        try {
            SeckillExecution execution = seckillService.executeSeckill(seckillId, phone, md5);
            return new SeckillResult<SeckillExecution>(true,execution);
        } catch (RepeatKillException e1) {
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            return new SeckillResult<SeckillExecution>(false,execution);
        } catch (SeckillCloseException e2) {
            SeckillExecution execution = new SeckillExecution(seckillId,SeckillStateEnum.END);
            return new SeckillResult<SeckillExecution>(false,execution);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            SeckillExecution execution =new SeckillExecution(seckillId,SeckillStateEnum.INNER_ERROR);
            return new SeckillResult<SeckillExecution>(false,execution);
        }
    }

    @RequestMapping(value = "/time/now",method = RequestMethod.GET)
    public SeckillResult<Long> time() {
        Date now = new Date();
        return new SeckillResult<Long>(true,now.getTime());
    }

}
