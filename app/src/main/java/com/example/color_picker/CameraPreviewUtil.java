package com.example.color_picker;

import android.hardware.Camera.Size;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraPreviewUtil {

    private CameraSizeComparator sizeComparator = new CameraSizeComparator();
    private static CameraPreviewUtil myCamPara = null;
    private CameraPreviewUtil(){

    }
    public static CameraPreviewUtil getInstance(){
        if(myCamPara == null){
            myCamPara = new CameraPreviewUtil();
            return myCamPara;
        }
        else{
            return myCamPara;
        }
    }

    public Size getPreviewSize(List<Size> list, float th, int minWidth){
        Collections.sort(list, sizeComparator);

        int i = 0;
        for(Size s:list){
            if((s.width > minWidth) && equalRate(s, th)){
                break;
            }
            i++;
        }
        return list.get(i);
    }

    public boolean equalRate(Size s, float rate){
        float r = (float)(s.width)/(float)(s.height);
        if(Math.abs(r - rate) <= 0.2)
        {
            return true;
        }
        else{
            return false;
        }
    }

    public class CameraSizeComparator implements Comparator<Size> {
        public int compare(Size lhs, Size rhs) {
            if(lhs.width == rhs.width){
                return 0;
            }
            else if(lhs.width > rhs.width){
                return 1;
            }
            else{
                return -1;
            }
        }
    }
}
