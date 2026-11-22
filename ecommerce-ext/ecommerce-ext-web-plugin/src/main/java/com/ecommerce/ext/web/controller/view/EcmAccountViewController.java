package com.ecommerce.ext.web.controller.view;

import org.youngmonkeys.ezyaccount.web.controller.view.AccountController;
import com.tvd12.ezyhttp.server.core.annotation.Authenticated;
import com.tvd12.ezyhttp.server.core.annotation.Controller;

/** Activates the inherited routes of its base controller. */
@Authenticated
@Controller
public class EcmAccountViewController extends AccountController {
}
