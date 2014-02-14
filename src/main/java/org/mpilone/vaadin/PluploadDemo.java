package org.mpilone.vaadin;

import static java.lang.String.format;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;

/**
 * Demo for the Plupload Vaadin component.
 *
 * @author mpilone
 */
@Theme("runo")
@Push(value = PushMode.DISABLED)
public class PluploadDemo extends UI {

  private final static String FULL_WIDTH = "100%";

  private TextArea logTxt;

  private VerticalLayout leftColumnLayout;

  @Override
  protected void init(VaadinRequest request) {

    setPollInterval(3000);
    setWidth(FULL_WIDTH);

    HorizontalLayout contentLayout = new HorizontalLayout();
    contentLayout.setMargin(true);
    contentLayout.setSpacing(true);
    contentLayout.setWidth(FULL_WIDTH);
    setContent(contentLayout);

    leftColumnLayout = new VerticalLayout();
    leftColumnLayout.setSpacing(true);
    contentLayout.addComponent(leftColumnLayout);

    // Upload 1: Manual submit button.
    Plupload upload = buildPlupload();
    addExample("Manual Submit", upload);

    // Upload 2: Immediate submit.
    upload = buildPlupload();
    upload.setImmediate(true);
    upload.setButtonCaption("Upload Now");
    addExample("Immediate Submit", upload);

    // Upload 3: Manual submit using flash.
    upload = buildPlupload();
    upload.setRuntimes(Plupload.Runtime.FLASH);
    upload.setImmediate(true);
    upload.setButtonCaption("Flash is Fun");
    addExample("Immediate Submit using Flash", upload);

    // Upload 4: Manual submit using html4.
    upload = buildPlupload();
    upload.setRuntimes(Plupload.Runtime.HTML4);
    upload.setButtonCaption("HTML4 is so Old");
    addExample("Manual Submit using HTML4", upload);

    // Upload 4: Immediate submit using sleep.
    upload = buildPlupload();
    upload.setButtonCaption("Immediate Submit Forced Slow");
    upload.setReceiver(new DemoReceiver(new SlowOutputStream()));
    upload.setImmediate(true);
    addExample("Slow it Down", upload);

    // The log text area.
    VerticalLayout rightColumnLayout = new VerticalLayout();
    rightColumnLayout.setSpacing(true);
    contentLayout.addComponent(rightColumnLayout);

    Label lbl = new Label("<h2>Upload Log</h2>", ContentMode.HTML);
    rightColumnLayout.addComponent(lbl);

    logTxt = new TextArea();
    logTxt.setRows(35);
    logTxt.setWidth(FULL_WIDTH);
    rightColumnLayout.addComponent(logTxt);
  }

  private void addExample(String title, Plupload upload) {
    ProgressBar progressBar = new ProgressBar();
    upload.addProgressListener(new BarProgressListener(progressBar));

    Label lbl = new Label("<h2>" + title + "</h2>", ContentMode.HTML);
    leftColumnLayout.addComponent(lbl);

    HorizontalLayout rowLayout = new HorizontalLayout();
    rowLayout.setSpacing(true);
    rowLayout.addComponent(upload);
    rowLayout.addComponent(progressBar);

    leftColumnLayout.addComponent(rowLayout);
  }

  private void log(String msg, Object... args) {

    if (args != null && args.length > 0) {
      msg = format(msg, args);
    }

    String value = logTxt.getValue();
    value = format("%s\n[%s] %s", value, new Date(), msg);
    logTxt.setValue(value);
    logTxt.setCursorPosition(value.length() - 1);
  }

  private Plupload buildPlupload() {

    final Plupload upload = new Plupload();
    upload.setChunkSize(512 * 1024);
    upload.setMaxRetryBufferSize(upload.getChunkSize() * 2);
    upload.setMaxFileSize(500 * 1024 * 1024);
    upload.setMaxRetries(2);
    upload.setButtonCaption("Upload File");
    upload.setReceiver(new DemoReceiver(new CountingDigestOutputStream()));

    upload.addStartedListener(new Plupload.StartedListener() {
      @Override
      public void uploadStarted(Plupload.StartedEvent evt) {
        log("Upload of file %s started with content size %d using "
            + "runtime %s.", evt.getFilename(), evt.getContentLength(), evt.
            getRuntime());

        upload.setEnabled(false);
      }
    });
    upload.addSucceededListener(new Plupload.SucceededListener() {
      @Override
      public void uploadSucceeded(Plupload.SucceededEvent evt) {
        log("Upload of file %s succeeded with size %d.", evt.
            getFilename(), evt.getLength());
      }
    });
    upload.addFinishedListener(new Plupload.FinishedListener() {
      @Override
      public void uploadFinished(Plupload.FinishedEvent evt) {
        upload.setEnabled(true);

        String hash = null;
        int count = 0;
        if (upload.getReceiver() instanceof DemoReceiver) {
          CountingDigestOutputStream o = ((DemoReceiver) upload.getReceiver()).
              getOutputStream();
          hash = o.getHash();
          count = o.getCount();
          o.reset();
        }

        log("Upload of file %s finished with reported size %d, "
            + "actual size %d, and MD5 hash %s.", evt.getFilename(),
            evt.getLength(), count, hash);
      }
    });

    return upload;
  }

  private class BarProgressListener implements
      Upload.ProgressListener {

    private final ProgressBar bar;

    public BarProgressListener(ProgressBar bar) {
      this.bar = bar;
    }

    @Override
    public void updateProgress(long readBytes, long contentLength) {
      if (readBytes % 2048 == 0 || readBytes == contentLength) {
        log("Read %d bytes of %d.", readBytes, contentLength);
      }

      if (contentLength > 0) {
        float percent = (float) readBytes / (float) contentLength;
        bar.setValue(percent);
      }
    }
  }

  private class CountingDigestOutputStream extends OutputStream {

    private int count = 0;
    private MessageDigest md;

    public CountingDigestOutputStream() {
      reset();
    }

    @Override
    public void write(int b) throws IOException {
      count++;
      md.update((byte) b);
    }

    public void reset() {
      count = 0;

      try {
        md = MessageDigest.getInstance("MD5");
      }
      catch (NoSuchAlgorithmException ex) {
        throw new RuntimeException("Unable to create MD5 digest.", ex);
      }
    }

    public int getCount() {
      return count;
    }

    public String getHash() {
      byte[] hash = md.digest();

      BigInteger bigInt = new BigInteger(1, hash);
      String hashText = bigInt.toString(16);
      while (hashText.length() < 32) {
        hashText = "0" + hashText;
      }

      return hashText;
    }
  }

  private class SlowOutputStream extends CountingDigestOutputStream {

    @Override
    public void write(byte[] b) throws IOException {

      if (getCount() % 1024 == 0) {
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException ex) {
          // ignore
        }
      }

      super.write(b);
    }

  }

  private class DemoReceiver implements Upload.Receiver {

    private final CountingDigestOutputStream outstream;

    public DemoReceiver(CountingDigestOutputStream outstream) {
      this.outstream = outstream;
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
      log("Creating receiver output stream for file %s and mime-type %s.",
          filename, mimeType);

      return outstream;
    }

    public CountingDigestOutputStream getOutputStream() {
      return outstream;
    }
  }


}
