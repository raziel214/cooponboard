package com.quimbaya.cooponboard.onboarding.domain.ports.out;

import java.io.InputStream;

public interface DocumentStoragePort {
    String upload(String objectName, String contentType, byte[] data);
    InputStream download(String objectName);
}
