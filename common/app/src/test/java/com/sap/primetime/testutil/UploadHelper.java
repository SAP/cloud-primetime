package com.sap.primetime.testutil;

import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

public class UploadHelper {

	public static void simulateFileUpload(HttpServletRequestMock requestMock, String fileName, String mimeType)
			throws IOException {

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder = builder.addBinaryBody(fileName, IOUtils.resourceToByteArray("/" + fileName),
				ContentType.create(mimeType), fileName);
		HttpEntity entity = builder.build();

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		entity.writeTo(bos);
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());

		when(requestMock.getMethod()).thenReturn("POST");
		when(requestMock.getContentType()).thenReturn(entity.getContentType().getValue());
		when(requestMock.getContentLength()).thenReturn((int) entity.getContentLength());
		when(requestMock.getInputStream()).thenReturn(new MockServletInputStream(bis));
	}

	public static class MockServletInputStream extends ServletInputStream {
		private final InputStream delegate;

		public MockServletInputStream(InputStream delegate) {
			this.delegate = delegate;
		}

		@Override
		public int read() throws IOException {
			return delegate.read();
		}

		@Override
		public boolean isFinished() {
			return true;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setReadListener(ReadListener listener) {
		}

	}
}