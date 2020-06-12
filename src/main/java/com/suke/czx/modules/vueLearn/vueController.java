package com.suke.czx.modules.vueLearn;

import com.suke.czx.common.annotation.AuthIgnore;
import com.suke.czx.common.base.AbstractController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/actuator")
public class vueController extends AbstractController {

    @AuthIgnore
    @RequestMapping(value = "/main")
    public String zxc(HttpServletRequest request) {
        return "";
    }
}
