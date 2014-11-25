package com.exercise.AndroidCamera;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;





import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AndroidCamera extends Activity implements SurfaceHolder.Callback {

	Camera camera;
	SurfaceView surfaceView;
	SurfaceHolder surfaceHolder;
	boolean previewing = false;
	LayoutInflater controlInflater = null;

	Button buttonTakePicture;//���հ�ť
	TextView prompt;//��ʾ
	DrawingView drawingView;//���ڻ����View
	Face[] detectedFaces;
	private String TAG = AndroidCamera.class.getName();

	TextToSpeech tts;
	
	final int RESULT_SAVEIMAGE = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// ��Ļ��������
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		getWindow().setFormat(PixelFormat.UNKNOWN);
		surfaceView = (SurfaceView) findViewById(R.id.camerapreview);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		drawingView = new DrawingView(this);
		LayoutParams layoutParamsDrawing = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		this.addContentView(drawingView, layoutParamsDrawing);

		controlInflater = LayoutInflater.from(getBaseContext());
		View viewControl = controlInflater.inflate(R.layout.control, null);
		LayoutParams layoutParamsControl = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		this.addContentView(viewControl, layoutParamsControl);

		buttonTakePicture = (Button) findViewById(R.id.takepicture);
		buttonTakePicture.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				camera.takePicture(myShutterCallback, myPictureCallback_RAW,
						myPictureCallback_JPG);
			}
		});

		LinearLayout layoutBackground = (LinearLayout) findViewById(R.id.background);
		layoutBackground.setOnClickListener(new LinearLayout.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				buttonTakePicture.setEnabled(false);
				camera.autoFocus(myAutoFocusCallback);
			}
		});

		prompt = (TextView) findViewById(R.id.prompt);
		
		//��ʼ��TTS
		tts = new TextToSpeech(this, new OnInitListener()
		{
			@Override
			public void onInit(int status)
			{
				// ���װ��TTS����ɹ�
				if (status == TextToSpeech.SUCCESS)
				{
					// ����ʹ����ʽӢ���ʶ�
					int result = tts.setLanguage(Locale.US);
					// �����֧�������õ�����
					if (result != TextToSpeech.LANG_COUNTRY_AVAILABLE
						&& result != TextToSpeech.LANG_AVAILABLE)
					{
						Toast.makeText(getApplicationContext(), "TTS��ʱ��֧���������Ե��ʶ���", Toast.LENGTH_LONG)
							.show();
					}
				}
			}

		});
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		// �ر�TextToSpeech����
				if (tts != null)
				{
					tts.shutdown();
				}
	}
	
	boolean faceDetect=false;
	//���ڴ��ת���������
	Rect rect=new Rect();
	
	
	
	FaceDetectionListener faceDetectionListener = new FaceDetectionListener() {
		@Override
		public void onFaceDetection(final Face[] faces, Camera camera) {
			/**	
			   **************************************************************
			   *					-1000							* BACK��  *
			   *						|							*		*
			   * 						|							*		*
			   * 						|							*		*
			   * 						|							*		*
			   * 						|							*		*
			   * -1000����������������������������������0.0������������������������������������+1000	* HOME��  *
			   * 						|							*		*
			   * 						|							*		*
			   * 						|							*		*
			   *						|							*		*
			   *						|							* MENU��  *
			   *						+1000						*		*
			   **************************************************************
			   */
			if (faces.length == 0) {
				prompt.setText(" No Face Detected! ");
				drawingView.setHaveFace(false);
				Log.i(TAG, " No Face Detected! ");
				if(faceDetect)//���֮ǰ������������ô���ڲ���¼��
					{
					tts.speak("δ��⵽����", TextToSpeech.QUEUE_ADD, null);
					faceDetect=!faceDetect;
					}
				
			} else {
				prompt.setText(String.valueOf(faces.length)
						+ " Face Detected :) ");
				drawingView.setHaveFace(true);
				detectedFaces = faces;
				Log.i(TAG, " Face Detected :) ");
				if(!faceDetect)//���֮ǰδ������������ô���ڲ���¼��
					{
					tts.speak("��⵽����", TextToSpeech.QUEUE_ADD, null);
					faceDetect=!faceDetect;
					}
				//����ת��
				rect.left=-faces[0].rect.bottom;
				rect.right=-faces[0].rect.top;
				rect.top=faces[0].rect.left;
				rect.bottom=faces[0].rect.right;
				
				int second=Calendar.getInstance().get(Calendar.SECOND);
				
				if(second%10==0)
				{
				if(Math.abs(rect.right-rect.left)>1300&&(Calendar.getInstance().get(Calendar.SECOND)%10==0))
				{
					tts.speak("����������������", TextToSpeech.QUEUE_ADD, null);
				}
				else if(Math.abs(rect.right-rect.left)<1000&&(Calendar.getInstance().get(Calendar.SECOND)%10==0))
				{
					tts.speak("��������������Զ", TextToSpeech.QUEUE_ADD, null);
				}
				}
				
				
			}
		}
	};

	AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback() {

		@Override
		public void onAutoFocus(boolean arg0, Camera arg1) {
			buttonTakePicture.setEnabled(true);
			// camera.startFaceDetection();
		}
	};

	ShutterCallback myShutterCallback = new ShutterCallback() {

		@Override
		public void onShutter() {
			// TODO Auto-generated method stub

		}
	};

	PictureCallback myPictureCallback_RAW = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] arg0, Camera arg1) {
			// TODO Auto-generated method stub

		}
	};

	PictureCallback myPictureCallback_JPG = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] arg0, Camera arg1) {
			// TODO Auto-generated method stub
			/*
			 * Bitmap bitmapPicture = BitmapFactory.decodeByteArray(arg0, 0,
			 * arg0.length);
			 */

			Uri uriTarget = getContentResolver().insert(
					Media.EXTERNAL_CONTENT_URI, new ContentValues());

			OutputStream imageFileOS;
			try {
				imageFileOS = getContentResolver().openOutputStream(uriTarget);
				imageFileOS.write(arg0);
				imageFileOS.flush();
				imageFileOS.close();

				prompt.setText("Image saved: " + uriTarget.toString());

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			camera.startPreview();
			camera.startFaceDetection();
		}
	};

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		if (previewing) {
			camera.stopFaceDetection();
			camera.stopPreview();
			previewing = false;
		}

		if (camera != null) {
			try {
				camera.setDisplayOrientation(90);
				camera.setPreviewDisplay(surfaceHolder);
				camera.startPreview();

				prompt.setText(String.valueOf("Max Face: "
						+ camera.getParameters().getMaxNumDetectedFaces()));
				int maxDetectFace = camera.getParameters()
						.getMaxNumDetectedFaces();
				if (maxDetectFace > 0)
					camera.startFaceDetection();
				previewing = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		camera = Camera.open();
		camera.setFaceDetectionListener(faceDetectionListener);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		camera.stopFaceDetection();
		camera.stopPreview();
		camera.release();
		camera = null;
		previewing = false;
	}

	private class DrawingView extends View {

		boolean haveFace;
		Paint drawingPaint;

		public DrawingView(Context context) {
			super(context);
			haveFace = false;
			drawingPaint = new Paint();
			drawingPaint.setColor(Color.GREEN);
			drawingPaint.setStyle(Paint.Style.STROKE);
			drawingPaint.setStrokeWidth(2);
		}

		public void setHaveFace(boolean h) {
			haveFace = h;
			invalidate();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			if (haveFace) {
				// Camera driver coordinates range from (-1000, -1000) to (1000,
				// 1000).
				// UI coordinates range from (0, 0) to (width, height).

				int vWidth = getWidth();
				int vHeight = getHeight();

				for (int i = 0; i < detectedFaces.length; i++) {
					switch (i) {
					case 0:
						drawingPaint.setColor(Color.GREEN);
						break;
					case 1:
						drawingPaint.setColor(Color.RED);
						break;
					case 2:
						drawingPaint.setColor(Color.YELLOW);
						break;
					case 3:
						drawingPaint.setColor(Color.BLUE);
						break;
					case 4:
						drawingPaint.setColor(Color.GRAY);
					case 5:
						drawingPaint.setColor(Color.LTGRAY);
					case 6:
						drawingPaint.setColor(Color.MAGENTA);
					case 7:
						drawingPaint.setColor(Color.WHITE);
						break;
					default:
						break;
					}

					int l = detectedFaces[i].rect.left;
					int t = detectedFaces[i].rect.top;
					int r = detectedFaces[i].rect.right;
					int b = detectedFaces[i].rect.bottom;

					// ����
					if (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT == getRequestedOrientation()) {
						int left = (l + 1000) * vHeight / 2000;
						int top = (t + 1000) * vWidth / 2000;
						int right = (r + 1000) * vHeight / 2000;
						int bottom = (b + 1000) * vWidth / 2000;
					

						canvas.drawRect(vWidth - bottom, left, vWidth - top,
								right, drawingPaint);
					}
					// ����
					else if (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE == getRequestedOrientation()) {
						int left = (l + 1000) * vWidth / 2000;
						int top = (t + 1000) * vHeight / 2000;
						int right = (r + 1000) * vWidth / 2000;
						int bottom = (b + 1000) * vHeight / 2000;
						
						canvas.drawRect(left, top, right, bottom, drawingPaint);
					}
				}
			} else {
				canvas.drawColor(Color.TRANSPARENT);
			}
		}
	}
}