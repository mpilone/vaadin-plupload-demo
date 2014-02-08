package org.mpilone.vaadin;

import static java.lang.String.format;

import java.io.IOException;
import java.io.OutputStream;
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

  private TextArea logTxt;

  private VerticalLayout contentLayout;

  @Override
  protected void init(VaadinRequest request) {

    setPollInterval(3000);

    contentLayout = new VerticalLayout();
    contentLayout.setSpacing(true);
    contentLayout.setMargin(true);
    setContent(contentLayout);

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
    upload.setRuntimes("flash,silverlight,html4");
    upload.setImmediate(true);
    upload.setButtonCaption("Flash is Fun");
    addExample("Immediate Submit using Flash", upload);

    // Upload 4: Manual submit using html4.
    upload = buildPlupload();
    upload.setRuntimes("html4");
    upload.setButtonCaption("HTML4 is so Old");
    addExample("Manual Submit using HTML4", upload);

    // Upload 4: Immediate submit using sleep.
    upload = buildPlupload();
    upload.setButtonCaption("Immediate Submit Forced Slow");
    upload.setReceiver(new SlowReceiver(10));
    upload.setImmediate(true);
    addExample("Slow it Down", upload);

    // The log text area.
    Label lbl = new Label("<h2>Upload Log</h2>", ContentMode.HTML);
    contentLayout.addComponent(lbl);

    logTxt = new TextArea();
    logTxt.setColumns(80);
    logTxt.setRows(15);
    contentLayout.addComponent(logTxt);
  }

  private void addExample(String title, Plupload upload) {
    ProgressBar progressBar = new ProgressBar();
    upload.addProgressListener(new BarProgressListener(progressBar));

    Label lbl = new Label("<h2>" + title + "</h2>", ContentMode.HTML);
    contentLayout.addComponent(lbl);

    HorizontalLayout rowLayout = new HorizontalLayout();
    rowLayout.setSpacing(true);
    rowLayout.addComponent(upload);
    rowLayout.addComponent(progressBar);

    contentLayout.addComponent(rowLayout);
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
    upload.setChunkSize(512l * 1024l);
    upload.setMaxFileSize(500 * 1024 * 1024);
    upload.setMaxRetries(2);
    upload.setButtonCaption("Upload File");
    upload.setReceiver(new NullReceiver());

    upload.addStartedListener(new Plupload.StartedListener() {
      @Override
      public void uploadStarted(Plupload.StartedEvent evt) {
        log("Upload of file %s started with content size %d using "
            + "runtime %s.", evt.getFilename(), evt.getContentLength(), upload.
            getActiveRuntime());

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
        log("Upload of file %s finished with size %d.", evt.getFilename(),
            evt.getLength());

        upload.setEnabled(true);
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
      if (readBytes % 2048 == 0) {
        log("Read %d bytes of %d.", readBytes, contentLength);
      }

      float percent = (float) readBytes / (float) contentLength;
      bar.setValue(percent);
    }
  }

  private class NullReceiver implements Upload.Receiver {

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
      log("Creating receiver output stream for file %s and mime-type %s.",
          filename, mimeType);
      return new OutputStream() {
        @Override
        public void write(int b) throws IOException {
        }
      };
    }
  }

  private class SlowReceiver implements Upload.Receiver {

    private final long sleepMillis;

    public SlowReceiver(long sleepMillis) {
      this.sleepMillis = sleepMillis;
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
      log("Creating receiver output stream for file %s and mime-type %s.",
          filename, mimeType);
      return new OutputStream() {
        private long count = 0;

        @Override
        public void write(int b) throws IOException {
          count++;

          if (count % 1024 == 0) {
            try {
              Thread.sleep(sleepMillis);
            }
            catch (InterruptedException ex) {
              // ignore
            }
          }
        }
      };
    }

  }

}
