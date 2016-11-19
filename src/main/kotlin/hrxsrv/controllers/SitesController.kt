package hrxsrv.controllers

import hrxsrv.services.Redis
import org.springframework.web.bind.annotation.*

@RestController
class SitesController {

    @RequestMapping(value = "/site/register", method = arrayOf(RequestMethod.POST))
    fun registerSite(@RequestBody body: RegisterSiteRequestBody): RegisterSiteResult {
        Redis.instance.set("sites:${body.hash}", body.json)
        return RegisterSiteResult(true)
    }
    data class RegisterSiteRequestBody(val json: String, val hash: String)
    data class RegisterSiteResult(val ok: Boolean)

    @RequestMapping(value = "/site", method = arrayOf(RequestMethod.GET))
    fun getSite(@RequestParam(value = "hash") hash: String): String {
        return Redis.instance.get("sites:$hash") ?: ""
    }

}