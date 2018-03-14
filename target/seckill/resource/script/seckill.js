// 存放主要的交互逻辑js代码
// JavaScript模块化
var seckill = {
    // 封装秒杀相关的Ajax的URL
    URL: {
        now : function() {
            return '/seckill/time/now';
        }
    },
    // 验证手机号
    validatePhone: function (phone) {
        if (phone && phone.length == 11 && !isNaN(phone)) {
            return true;
        } else {
            return false;
        }
    },
    countdown : function(seckillid,nowTime,startTime,endTime) {
        // 时间判断
        if(nowTime > endTime) {
            // 秒杀结束
            seckillBox.html('秒杀结束！');
        } else if(nowTime<startTime) {
            // 秒杀未开始， 计时事件绑定
            var killTime = new Date(startTime + 1000);
            seckillBox.countdown(killTime,function (event) {
                // 时间格式
                var format = event.strftime('秒杀倒计时 ：%D天  %H时 %M分 %S秒 ');
                seckillBox.html(format);
            });
        } else {
            // 秒杀开始

        }
    },
    // 详情页秒杀逻辑
    detail: {
        // 详情页初始化
        init: function (params) {
            // 手机验证和登录, 计时交互
            // 规划交互流程
            // 在cookie中查找手机号
            var killPhone = $.cookie('killPhone');
            var startTime = params['startTime'];
            var endTime = params['endTime'];
            var seckillId = params['seckillId'];
            // 验证手机号
            if (!seckill.validatePhone(killPhone)) {
                // 绑定phone
                // 控制输出
                var killPhoneModal = $('#killPhoneModal');
                killPhoneModal.modal({
                    show: true, // 显示弹出层
                    backdrop: 'static', // 禁止位置关闭
                    keyboard: false // 关闭键盘事件
                });
                $('#killPhoneBtn').click(function () {
                    var inputPhone = $('#killPhoneKey').val();
                    if (seckill.validatePhone(inputPhone)) {
                        // 电话写入Cookie
                        $.cookie('killPhone', inputPhone, {expires: 7, path: '/seckill'});
                        // 验证通过，刷新页面
                        window.location.reload();
                    } else {
                        $('#killPhoneMessage').hide().html('<label class="label label-danger">手机号错误！！</label>').show(300);
                    }
                });
            }
            // 已经登录
            // 计时交互
            $.get(seckill.URL.now(),{},function(result) {
                if (result && result['success']) {
                    var nowTime = result['data'];
                    // 时间判断,计时交互
                    seckill.countdown(seckillId,nowTime,startTime,endTime);
                } else {
                    console.log('result : '+ result);
                }
            });
        }
    }
}