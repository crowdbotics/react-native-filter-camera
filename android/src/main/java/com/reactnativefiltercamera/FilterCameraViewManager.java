package com.reactnativefiltercamera;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import com.facebook.react.uimanager.ViewGroupManager ;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.SimpleViewManager;


import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;

import java.io.File;
import android.util.Log;
import androidx.annotation.NonNull;
import com.otaliastudios.cameraview.CameraUtils;
import android.net.Uri;


import com.otaliastudios.cameraview.size.SizeSelector;
import com.otaliastudios.cameraview.size.SizeSelectors;
import com.otaliastudios.cameraview.size.AspectRatio;


import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.FileCallback;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.filter.Filter;
import com.otaliastudios.cameraview.filter.Filters;
import com.otaliastudios.cameraview.filter.MultiFilter;
import com.otaliastudios.cameraview.filters.BrightnessFilter;
import com.otaliastudios.cameraview.filters.ContrastFilter;


public class FilterCameraViewManager extends ViewGroupManager<CameraView> {

    public static final String REACT_CLASS = "FilterCameraView";
    ReactApplicationContext mCallerContext;


    private CameraView cameraView;

    private ContrastFilter contrastFilter;
    private BrightnessFilter brightnessFilter;
    private String fileName;
  
    public FilterCameraViewManager(ReactApplicationContext reactContext) {
      mCallerContext = reactContext;
      contrastFilter=new ContrastFilter();
      brightnessFilter=new BrightnessFilter();
    }
    
    @Override
    public String getName() {
      return REACT_CLASS;
    }

    @Override
    public CameraView createViewInstance(ThemedReactContext context) {
      if(cameraView!=null)
      {
        cameraView.destroy();
      }
      cameraView=new CameraView(context);      
      onReceiveNativeEvent(context, cameraView);
      cameraView.setLifecycleOwner((AppCompatActivity)context.getCurrentActivity());               

      

    return cameraView;
  }

    @Override
    public void receiveCommand(CameraView view, String commandId, @Nullable ReadableArray args) {
      super.receiveCommand(view, commandId, args);
      switch (commandId) {
        case "switchCameraDirection":
            switchCameraDirection(view);
          break;
        case "switchFlash":
            switchFlash(view);
          break;
        case "setContrast":
            if (args != null) {
              double value=args.getDouble(0);              
              setContrast(view,value);
            }            
        break;
        case "setBrightness":
            if (args != null) {
              double value=args.getDouble(0);              
              setBrightness(view,value);
            }            
        break;
        case "takePicture":
            if (args != null) {
              String value=args.getString(0); 
              takePicture(view,value);         
            }       
        break;
        case "toggleVideo": 
            if (args != null) {              
              String value=args.getString(0); 
              takeVideo(view,value);         
            }       
        break;
        case "ratio":
            if(args != null){
              int width=args.getInt(0);
              int height=args.getInt(1);
              setSize(view,width,height);
            }
      }      
    }

    public void onReceiveNativeEvent(final ThemedReactContext reactContext, final CameraView cameraView) {
      cameraView.addCameraListener(new CameraListener() {
    
        @Override
        public void onPictureTaken(@NonNull PictureResult result) {
          String extension="";
          switch (result.getFormat()) {
            case JPEG: extension = "jpg"; break;
            case DNG: extension = "dng"; break;
            default: throw new RuntimeException("Unknown format.");
          }
          final int width=result.getSize().getWidth();
          final int height=result.getSize().getHeight();
          File f=new File(mCallerContext.getFilesDir(),fileName+"."+extension);
          f.setReadable(true);
          f.setWritable(true);
            // A Picture was taken!
            CameraUtils.writeToFile(result.getData(),f, new FileCallback() {
              @Override
              public void onFileReady(@Nullable File file) {
                  if (file != null) {     
                    WritableMap event = Arguments.createMap();
                    event.putString("uri", file.toURI()+"");
                    event.putString("name", file.getName());
                    event.putString("type", "image/jpeg");
                    event.putInt("width", width);      
                    event.putInt("height", height);            

                    reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(cameraView.getId(), "topChange", event);
                  } else {                      
                    Log.d("TEST","Error while writing file.");
                  }
              }
          });
            Log.d("TEST","onPictureTaken");
        }
        
        @Override
        public void onVideoTaken(@NonNull VideoResult result) {
            int width=result.getSize().getWidth();
            int height=result.getSize().getHeight();
            WritableMap event = Arguments.createMap();
            event.putString("uri", result.getFile().toURI()+"");   
            event.putString("name", result.getFile().getName());
            event.putString("type", "video/mp4");    
            event.putInt("width", width);      
            event.putInt("height", height);                        
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(cameraView.getId(), "topChange", event);
        }
        
        @Override
        public void onVideoRecordingStart() {
            // Notifies that the actual video recording has started.
            // Can be used to show some UI indicator for video recording or counting time.
            Log.d("TEST","onVideoRecordingStart");

        }
        
        @Override
        public void onVideoRecordingEnd() {
          Log.d("TEST","onVideoRecordingEnd");

            // Notifies that the actual video recording has ended.
            // Can be used to remove UI indicators added in onVideoRecordingStart.
        }
    });      
  }

    private void takeVideo(CameraView view,String value){
      view.setMode(Mode.VIDEO);
      if(view.isTakingVideo() == false){
          view.takeVideo(new File(mCallerContext.getFilesDir(),value+".mp4"));
      }else if(view.isTakingVideo() == true){
          view.stopVideo();
      }
    }
    private void takePicture(CameraView view, String value){
      view.setMode(Mode.PICTURE); // for pictures
      fileName=value;
      view.takePicture();
    }

    private void switchFlash(CameraView view){
          if(view.getFlash() == Flash.OFF){
            view.setFlash(Flash.TORCH);
        }else if(view.getFlash() == Flash.TORCH){
          view.setFlash(Flash.OFF);
        }
    }


    private void switchCameraDirection(CameraView view) {
        if(view.getFacing() == Facing.FRONT){
          view.setFacing(Facing.BACK);
        }else if(view.getFacing() == Facing.BACK){
          view.setFacing(Facing.FRONT);
        }
    }

    private void setContrast(CameraView view,double progress) {        
        contrastFilter.setContrast((float)progress);
        cameraView.setFilter(new MultiFilter(brightnessFilter, contrastFilter));
    }

    private void setBrightness(CameraView view,double progress) {
        brightnessFilter.setBrightness((float)progress);
        cameraView.setFilter(new MultiFilter(brightnessFilter, contrastFilter));
    }

    private void setSize(CameraView view, int widthRatio, int heightRatio){
      SizeSelector width = SizeSelectors.minWidth(1000);
      SizeSelector height = SizeSelectors.minHeight(2000);
      SizeSelector dimensions = SizeSelectors.and(width, height); // Matches sizes bigger than 1000x2000.
      SizeSelector ratio = SizeSelectors.aspectRatio(AspectRatio.of(widthRatio, heightRatio), 0); 

      SizeSelector result = SizeSelectors.or(
          SizeSelectors.and(ratio, dimensions), // Try to match both constraints
          ratio, // If none is found, at least try to match the aspect ratio
          SizeSelectors.biggest() // If none is found, take the biggest
      );
      view.setPictureSize(result);
      view.setVideoSize(result);

    }
}