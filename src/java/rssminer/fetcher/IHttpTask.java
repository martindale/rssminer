/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.fetcher;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;

public interface IHttpTask {

    URI getUri();

    Map<String, Object> getHeaders();

    Object doTask(int status, Map<String, String> headers, String body);

    Object onThrowable(Throwable t);

    Proxy getProxy();
}
