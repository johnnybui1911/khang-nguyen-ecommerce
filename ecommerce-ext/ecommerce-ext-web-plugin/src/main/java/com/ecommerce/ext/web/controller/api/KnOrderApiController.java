package com.ecommerce.ext.web.controller.api;

import org.youngmonkeys.ecommerce.web.controller.api.WebApiOrderController;
import com.tvd12.ezyhttp.server.core.annotation.Api;
import com.tvd12.ezyhttp.server.core.annotation.Authenticated;
import com.tvd12.ezyhttp.server.core.annotation.Controller;

/** Activates the inherited routes of its base controller. */
@Api
@Authenticated
@Controller("/api/v1")
public class KnOrderApiController extends WebApiOrderController {
}
