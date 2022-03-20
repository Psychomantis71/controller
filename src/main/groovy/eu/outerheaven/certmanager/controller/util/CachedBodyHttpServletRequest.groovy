package eu.outerheaven.certmanager.controller.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.StreamUtils

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(CachedBodyHttpServletRequest)

    private byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        InputStream requestInputStream = request.getInputStream();
        this.cachedBody = StreamUtils.copyToByteArray(requestInputStream);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        LOG.info("I tried to read data from the modified HttpServeletRequest")
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
        return new BufferedReader(new InputStreamReader(byteArrayInputStream));
    }

}

