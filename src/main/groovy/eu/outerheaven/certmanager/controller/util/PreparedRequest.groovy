package eu.outerheaven.certmanager.controller.util

import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.InstanceAccessData
import eu.outerheaven.certmanager.controller.form.AuthRequestForm
import eu.outerheaven.certmanager.controller.form.InstanceForm
import eu.outerheaven.certmanager.controller.repository.InstanceRepository
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

import javax.servlet.http.HttpServletRequest
import javax.xml.ws.spi.http.HttpExchange

class PreparedRequest {

    private static final Logger LOG = LoggerFactory.getLogger(PreparedRequest.class)


    private void getLoginToken(Instance instance){

        String url = instance.getAccessUrl() + "/login"

        RestTemplate template = new RestTemplate();
        AuthRequestForm authRequestForm = new AuthRequestForm()
        authRequestForm.setPassword(instance.getUser().getPassword())
        authRequestForm.setUsername(instance.getUser().getUserName())
        HttpEntity<AuthRequestForm> request = new HttpEntity<>(authRequestForm);
        HttpEntity<String> response = template.exchange(url, HttpMethod.POST, request, String.class);
        HttpHeaders headers = response.getHeaders();
        String xsrf_token = headers.get(HttpHeaders.SET_COOKIE).get(0)
        String cookie_bearer = headers.get(HttpHeaders.SET_COOKIE).get(1)
        String expires = headers.get(HttpHeaders.EXPIRES)
        expires= StringUtils.substringBetween(expires,"[", "]")
        xsrf_token = StringUtils.substringBetween(xsrf_token,"XSRF-TOKEN=", ";")
        cookie_bearer = StringUtils.substringBetween(cookie_bearer,"COOKIE-BEARER=", ";")

        if(instance.getInstanceAccessData() == null){
            InstanceAccessData instanceAccessData = new InstanceAccessData(
                    instance: instance,
                    xsrf_token: xsrf_token,
                    bearer_token: cookie_bearer,
                    expires: expires.toLong()
            )
            instance.setInstanceAccessData(instanceAccessData)
        }else {
            InstanceAccessData instanceAccessData = instance.getInstanceAccessData()
            instanceAccessData.setBearer_token(cookie_bearer)
            instanceAccessData.setXsrf_token(xsrf_token)
            instanceAccessData.setExpires(expires.toLong())
            instance.setInstanceAccessData(instanceAccessData)
        }
    }

    HttpHeaders getHeader(Instance instance){

        if(instance.getInstanceAccessData().getExpires() == null){
            LOG.debug("Token not found, requesting new")
            getLoginToken(instance)
        }else if(instance.getInstanceAccessData().getExpires() <= System.currentTimeMillis()){
            LOG.debug("Token expired, requesting new")
            getLoginToken(instance)
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.add("X-XSRF-TOKEN", instance.getInstanceAccessData().getXsrf_token())
        headers.add("Cookie","COOKIE-BEARER=" + instance.getInstanceAccessData().getBearer_token() + "; XSRF-TOKEN=" + instance.getInstanceAccessData().getXsrf_token())
        return headers

    }

    private static final String[] IP_HEADER_CANDIDATES = {
        "X-Forwarded-For"
        "Proxy-Client-IP"
        "WL-Proxy-Client-IP"
        "HTTP_X_FORWARDED_FOR"
        "HTTP_X_FORWARDED"
        "HTTP_X_CLUSTER_CLIENT_IP"
        "HTTP_CLIENT_IP"
        "HTTP_FORWARDED_FOR"
        "HTTP_FORWARDED"
        "HTTP_VIA"
        "REMOTE_ADDR"
    }

    String getClientIpAddressIfServletRequestExist(HttpServletRequest request) {

        if (RequestContextHolder.getRequestAttributes() == null) {
            return "0.0.0.0";
        }

        //HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        for (String header: IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && ipList.length() != 0 && !"unknown".equalsIgnoreCase(ipList)) {
                String ip = ipList.split(",")[0];
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    Instance determineInstance(HttpServletRequest request){
        String ip = getClientIpAddressIfServletRequestExist(request)

    }

}
