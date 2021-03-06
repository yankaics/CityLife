package com.common.media;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.common.widget.ToastHelper;

import net.tsz.afinal.utils.Utils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff.Mode;
import android.media.ExifInterface;
import android.os.Environment;

public class BitmapHelper {
	/**
	 * 读取图片属性：旋转的角度
	 * 
	 * @param path
	 *            图片绝对路径
	 * @return degree旋转的角度
	 */
	public static int readPictureDegree(String path) {
		int degree = 0;
		try {
			ExifInterface exifInterface = new ExifInterface(path);
			int orientation = exifInterface.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				degree = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				degree = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				degree = 270;
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return degree;
	}
	
	/*
	* 旋转图片 
    * @param angle 
    * @param bitmap 
    * @return Bitmap 
    */  
   public static Bitmap rotaingImageView(int angle, float scale, Bitmap bitmap) {  
       //旋转图片 动作  
       Matrix matrix = new Matrix(); 
       matrix.postRotate(angle);  
       matrix.postScale(scale, scale);

       // 创建新的图片  
       Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,  
               bitmap.getWidth(), bitmap.getHeight(), matrix, true);  
       return resizedBitmap;  
   }	
   
   /**
    * 获取压缩后的图
    */
   public static Bitmap getScaleBitmap(String picPath, int outWidth) {
	   BitmapFactory.Options options = new BitmapFactory.Options();
	   options.inJustDecodeBounds = true;
	   BitmapFactory.decodeFile(picPath, options);
	   
	   int height = options.outHeight  * outWidth /  options.outWidth;
	   options.inSampleSize = options.outWidth / outWidth;
	   options.outWidth = outWidth;
	   options.outHeight = height;
	   options.inJustDecodeBounds = false;
	   options.inPreferredConfig = Bitmap.Config.RGB_565;
	   
	   return BitmapFactory.decodeFile(picPath, options);
   }
   
   /**
    * 获取压缩后的图片并保存在SD卡中
    * @param context
    * @param picPath
    * @param outWidth
    * @return
    */
   public static File getScaleBitmapFile(Context context, String picPath, int outWidth) {	   	   
	   BitmapFactory.Options options = new BitmapFactory.Options();
	   options.inJustDecodeBounds = true;
	   BitmapFactory.decodeFile(picPath, options);
	   
	   int width = options.outWidth < outWidth ? options.outWidth : outWidth;	   
	   int height = options.outHeight  * width /  options.outWidth;
	   float scale = (float)width / options.outWidth;
	   options.inSampleSize = (int) scale;
	   options.outWidth = width;
	   options.outHeight = height;
	   options.inJustDecodeBounds = false;
	   options.inPreferredConfig = Bitmap.Config.RGB_565;	   
	   Bitmap bitmap = BitmapFactory.decodeFile(picPath, options);
	   	   	   
	   int degree = BitmapHelper.readPictureDegree(picPath);	
	   Bitmap roateBmp = BitmapHelper.rotaingImageView(degree, scale, bitmap);
	   
	   return saveBitmap(context, roateBmp);
   }
   
   /**
    * 保存位图
    * @param context
    * @param bm
    * @return
    */
   public static File saveBitmap(Context context, Bitmap bm) {	       
       String cachePath = Utils.getDiskCacheDir(context, "imgCache").getAbsolutePath();
       File mediaStorageDir = new File(cachePath);  
       
       if (!mediaStorageDir.exists()) {  
           if (!mediaStorageDir.mkdirs()) {  
        	   ToastHelper.showToastInBottom(context, "创建目录失败：" + cachePath);
               return null;  
           }  
       }  
       
       String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()); 
       File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "tmp_img" + timeStamp + ".jpg");
       try {
    	   FileOutputStream out = new FileOutputStream(mediaFile);
    	   bm.compress(Bitmap.CompressFormat.JPEG, 90, out);
    	   out.flush();
    	   out.close();
    	   
    	   return mediaFile;    	   
       } catch (FileNotFoundException e) {    
    	   e.printStackTrace();
    	   ToastHelper.showToastInBottom(context, "文件未找到");
       } catch (IOException e) {
    	   ToastHelper.showToastInBottom(context, "IO异常");
    	   e.printStackTrace();
       }
       
       return null;
   }
   
   public static Bitmap toRoundCorner(Bitmap bitmap, float radius) {  
       Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),  
               bitmap.getHeight(), Bitmap.Config.ARGB_8888);  
       Canvas canvas = new Canvas(output);  
 
       final Paint paint = new Paint();  
       final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());  
       final RectF rectF = new RectF(rect);  
 
       paint.setAntiAlias(true);  
       canvas.drawARGB(0, 0, 0, 0);  
       canvas.drawRoundRect(rectF, radius, radius, paint);  
       paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN)); 
       canvas.drawBitmap(bitmap, rect, rect, paint);   
 
       return output;  
 
   }
}
