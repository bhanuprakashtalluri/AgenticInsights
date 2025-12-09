package org.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class FrontendController {
    @RequestMapping(value = { "/", "/{path:^(?!api|static|favicon\\.ico$).*$}", "/{path:^(?!api|static|favicon\\.ico$).*$}/**" })
    public String getIndex() {
        return "forward:/index.html";
    }
}
