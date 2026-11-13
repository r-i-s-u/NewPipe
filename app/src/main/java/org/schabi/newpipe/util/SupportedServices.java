package org.schabi.newpipe.util;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;

import java.util.Collections;
import java.util.List;

/**
 * YT'm supports only YouTube (which includes YouTube Music).
 */
public final class SupportedServices {
    private SupportedServices() {
    }

    public static List<StreamingService> all() {
        return Collections.singletonList(ServiceList.YouTube);
    }
}
