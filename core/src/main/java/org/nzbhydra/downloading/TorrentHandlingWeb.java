package org.nzbhydra.downloading;

import com.google.common.io.Files;
import org.nzbhydra.GenericResponse;
import org.nzbhydra.api.WrongApiKeyException;
import org.nzbhydra.config.BaseConfig;
import org.nzbhydra.config.ConfigProvider;
import org.nzbhydra.config.NzbAccessType;
import org.nzbhydra.misc.UserAgentMapper;
import org.nzbhydra.searching.searchrequests.SearchRequest.SearchSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Objects;

@RestController
public class TorrentHandlingWeb {

    private static final Logger logger = LoggerFactory.getLogger(TorrentHandlingWeb.class);

    @Autowired
    private NzbHandler nzbHandler;
    @Autowired
    private ConfigProvider configProvider;
    @Autowired
    private UserAgentMapper userAgentMapper;

    /**
     * Provides an internal access to torrents via GUID
     *
     * @return A {@link ResponseEntity} with the torrent content, a redirect to the actual indexer link or an error
     */
    @RequestMapping(value = "/internalapi/torrent/{guid}", produces = "application/x-bittorrent")
    @Secured({"ROLE_USER"})
    public ResponseEntity<String> downloadTorrentInternal(@PathVariable("guid") long guid) {
        return nzbHandler.getNzbByGuid(guid, configProvider.getBaseConfig().getSearching().getNzbAccessType(), SearchSource.INTERNAL).getAsResponseEntity();
    }


    /**
     * Provides an external access to torrent via GUID for users.
     *
     * @return A {@link ResponseEntity} with the torrent content, a redirect to the actual indexer link or an error
     */
    @RequestMapping(value = "/gettorrent/user/{guid}", produces = "application/x-bittorrent")
    @Secured({"ROLE_USER"})
    public ResponseEntity<String> downloadTorrentForUsers(@PathVariable("guid") long guid) {
        return nzbHandler.getNzbByGuid(guid, configProvider.getBaseConfig().getSearching().getNzbAccessType(), SearchSource.INTERNAL).getAsResponseEntity();
    }

    /**
     * Provides an external access to torrent via GUID for users.
     *
     * @return A {@link ResponseEntity} with the torrent content, a redirect to the actual indexer link or an error
     */
    @RequestMapping(value = "/internalapi/saveTorrent/{guid}")
    @Secured({"ROLE_USER"})
    public GenericResponse sentTorrentToBlackhole(@PathVariable("guid") long guid) {
        NzbDownloadResult downloadResult = nzbHandler.getNzbByGuid(guid, NzbAccessType.PROXY, SearchSource.INTERNAL);
        if (!downloadResult.isSuccessful()) {
            return GenericResponse.notOk(downloadResult.getError());
        }
        if (!configProvider.getBaseConfig().getDownloading().getSaveTorrentsTo().isPresent()) {
            logger.error("Torrent black hole folder not set");
            return GenericResponse.notOk("Torrent black hole folder not set");
        }
        String sanitizedTitle = downloadResult.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
        if(!Objects.equals(sanitizedTitle, downloadResult.getTitle())) {
            logger.info("Sanitized torrent title from '{}' to '{}'", downloadResult.getTitle(), sanitizedTitle);
        }
        File torrent = new File(configProvider.getBaseConfig().getDownloading().getSaveTorrentsTo().get(), sanitizedTitle + ".torrent");
        try {
            Files.write(downloadResult.getNzbContent().getBytes(), torrent);
            logger.info("Saved torrent file to {}", torrent.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error saving torrent file", e);
            return GenericResponse.notOk("Error saving torrent file: " + e.getMessage());
        }
        return GenericResponse.ok();
    }

    /**
     * Provides an external access to torrents via GUID
     *
     * @return A {@link ResponseEntity} with the torrent content, a redirect to the actual indexer link or an error
     */
    @RequestMapping(value = "/gettorrent/api/{guid}", produces = "application/x-bittorrent")
    public ResponseEntity<String> downloadTorrentWithApikey(@PathVariable("guid") long guid, @RequestParam(required = false) String apikey) throws WrongApiKeyException {
        BaseConfig baseConfig = configProvider.getBaseConfig();
        if (apikey == null || !apikey.equals(baseConfig.getMain().getApiKey())) {
            logger.error("Received torrent API download call with wrong API key");
            throw new WrongApiKeyException("Wrong api key");
        }

        return nzbHandler.getNzbByGuid(guid, baseConfig.getSearching().getNzbAccessType(), SearchSource.INTERNAL).getAsResponseEntity();
    }

}