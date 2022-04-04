package eu.outerheaven.certmanager.controller.util

import eu.outerheaven.certmanager.controller.util.CachedBodyHttpServletRequest

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

class CustomFilter implements Filter {
    @Override
    void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        //TODO this causes every request to be read twice, while only needed for login, needs to be optimised
        /* wrap the request in order to read the inputstream multiple times */
        CachedBodyHttpServletRequest multiReadRequest = new CachedBodyHttpServletRequest((HttpServletRequest) request);

        /* here I read the inputstream and do my thing with it; when I pass the
         * wrapped request through the filter chain, the rest of the filters, and
         * request handlers may read the cached inputstream
         */
        //doMyThing(multiReadRequest.getInputStream())
        //OR
        //anotherUsage(multiReadRequest.getReader());
        chain.doFilter(multiReadRequest, response);
    }
}