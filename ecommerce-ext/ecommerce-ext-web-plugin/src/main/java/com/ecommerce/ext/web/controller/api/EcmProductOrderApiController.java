package com.ecommerce.ext.web.controller.api;

import org.youngmonkeys.ecommerce.web.controller.api.WebApiProductOrderController;
import com.tvd12.ezyhttp.server.core.annotation.Api;
import com.tvd12.ezyhttp.server.core.annotation.Controller;

/** Activates the inherited routes of its base controller. */
@Api
@Controller("/api/v1")
public class EcmProductOrderApiController extends WebApiProductOrderController {
}
