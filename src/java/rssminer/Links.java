package rssminer;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

/**
 * executed too often, it's better to be implemented for performance
 * 
 * @author feng
 * 
 */
public class Links {
    public static class LinksConf {
        List<String> ignoredExtensions;
        List<Pattern> badDomainPattens;
        List<String> blackDomainStr;
        List<String> acceptedTopDomains;

        public void setIgnoredExtensions(List<String> ignoredExtensions) {
            this.ignoredExtensions = ignoredExtensions;
        }

        public void setBadDomainPattens(List<Pattern> badDomainPattens) {
            this.badDomainPattens = badDomainPattens;
        }

        public void setBlackDomainStr(List<String> blackDomainStr) {
            this.blackDomainStr = blackDomainStr;
        }

        public void setAcceptedTopDomains(List<String> acceptedTopDomains) {
            this.acceptedTopDomains = acceptedTopDomains;
        }
    }

    final String[] mIgnoredExtensions;
    final Pattern[] mBadDomainPattens;
    final String[] mBlackWords;
    final String[] mAcceptedTopDomains;

    public Links(LinksConf conf) {
        mIgnoredExtensions = new String[conf.ignoredExtensions.size()];
        conf.ignoredExtensions.toArray(mIgnoredExtensions);

        mBadDomainPattens = new Pattern[conf.badDomainPattens.size()];
        conf.badDomainPattens.toArray(mBadDomainPattens);

        mBlackWords = new String[conf.blackDomainStr.size()];
        conf.blackDomainStr.toArray(mBlackWords);

        mAcceptedTopDomains = new String[conf.acceptedTopDomains.size()];
        conf.acceptedTopDomains.toArray(mAcceptedTopDomains);
    }

    public boolean keep(URI uri) {

        if (uri.getScheme() == null || !uri.getScheme().startsWith("http"))
            return false;

        String host = uri.getHost();

        boolean keep = false;
        for (String topDomain : mAcceptedTopDomains) {
            if (host.endsWith(topDomain)) {
                keep = true;
                break;
            }
        }

        if (!keep)
            return false;

        String path = uri.getPath();
        for (String extension : mIgnoredExtensions) {
            if (path.endsWith(extension)) {
                // in the black list, return early
                return false;
            }
        }

        for (String black : mBlackWords) {
            if (host.contains(black)) {
                return false;
            }
        }

        for (Pattern bad : mBadDomainPattens) {
            if (bad.matcher(host).find()) {
                return false;
            }
        }

        return true;
    }

    public URI resoveAndClean(String base, String part) {

        URI result = resolve(base, part);
        if (result != null && keep(result)) {
            return result;
        } else {
            return null;
        }
    }

    public URI resolve(String base, String part) {
        try {
            URI uri = new URI(base);
            if (uri.getPath().length() == 0) {
                return uri.resolve("/" + part);
            } else {
                return uri.resolve(part);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
