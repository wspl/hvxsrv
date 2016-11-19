package hrxsrv.services

import redis.clients.jedis.Jedis

object Redis {
    private var _instance: Jedis? = null
    val instance: Jedis
        get() {
            if (_instance == null) {
                _instance = Jedis("localhost")
            }
            return _instance as Jedis
        }
}