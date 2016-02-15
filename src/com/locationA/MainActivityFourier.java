package com.locationA;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.locationA.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus.Listener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

@SuppressLint("SdCardPath")
public class MainActivityFourier extends Activity implements SensorEventListener {
    public SensorManager mSensorManager;
    public Sensor mAccelerometer;
    TextView title,tvx,tvy,tvz,tvh,numOfCompetions;
    EditText etshowval;
    RelativeLayout layout;
    boolean minimized = true;
    public BufferedWriter mBufferedWriter;
    public BufferedReader mBufferedReader;
    private double x,y,z;
    public int numOfWin;
    private long timeS1,timeS2;
    Listener fastestListener;
    //note I'm only making copies so i can verify the output at each stage
    public double[] arrayWindowx = new double[256];
    public double[] arrayWindowy = new double[256];
    public double[] arrayWindowz = new double[256];

    public int file_counter;
    
    public double[] arrayWindowx2 = new double[512];
    public double[] arrayWindowy2 = new double[512];
    public double[] arrayWindowz2 = new double[512];


    

    //these 'truth' values were generated after averaging a large collection of ESD spread samples for
    //each axis [x,y,z]
    public final double[] legTruth = {0.213959556425310	,0.0728419221569907,0.235268536638400};
    public final double[] chestTruth = 	  {0.198847575559904	,0.0478054235392787	,0.191950991236280};
    public final double[] armTruth=	  {0.0677592954990215	,0.0599315068493151,	0.0641487279843444};
    
    // String array isn't necessary, efficiency isn't a concern here, so leaving as is.
    public final String[] location = {"leg","chest","arm"};
    
    int windowTic = 0;
    int windowTic2 = 0;
    private String read_str = "";
    
    MediaPlayer mPlayer,mPlayer2,mPlayer3;
    
    public boolean mrunning = false;
    //public Handler mHandler;  //commented out on 2/14/16
    DoubleFFT_1D fft= new DoubleFFT_1D(511);
    DoubleFFT_1D fft2= new DoubleFFT_1D(1023);
    /** Called when the activity is first created. */
    @SuppressLint("InlinedApi")
    @Override
    public void onCreate(Bundle savedInstanceState)     {

        file_counter = 0;
    	
    	mPlayer3 = MediaPlayer.create(this, R.raw.rumble);
    	mPlayer3.start();  
    	mPlayer3.setVolume((float)0.5,(float) 0.5);
    	
    	

        //forces the screen into landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main_fourier);
        
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        
        //get layout
        layout = (RelativeLayout) findViewById(R.id.relative);
        View modifyButton = (View)findViewById(R.id.button1);
        modifyButton.setBackgroundColor(Color.GREEN);
        
        //get textviews
        title = (TextView)findViewById(R.id.name);
        tvx = (TextView)findViewById(R.id.xval);
        tvy = (TextView)findViewById(R.id.yval);
        tvz = (TextView)findViewById(R.id.zval);
        tvh = (TextView)findViewById(R.id.herz1);
        numOfCompetions = (TextView)findViewById(R.id.numbeOfCalulations);
        numOfCompetions.setText("stopped");
        numOfCompetions.setTextColor(Color.RED);
        numOfCompetions.setTextSize(15);
    }
    @SuppressLint("SdCardPath")
    public void onStartClick(View view)
    {
        //plays music so the user can easily walk at a consistent beat of 120 bpm (two steps per second)
        //This was introduced so account for the fact that the ESD spread of each accelerometer axis output
        //will change based on the speed that the user is walking
    	mPlayer = MediaPlayer.create(this, R.raw.grill);
    	mPlayer.start();  
    	mPlayer.setVolume((float)0.2,(float) 0.2);
    	
        file_counter ++;
        numOfWin = 1;
        timeS2=0;


        TextView compl = (TextView) findViewById(R.id.completion);
        compl.setText("runnig");
        compl.setTextSize(15);
        View modifyButton = (View)findViewById(R.id.button2);
        modifyButton.setBackgroundColor(Color.RED);
        View modifyButtont = (View)findViewById(R.id.button1);
        modifyButtont.setBackgroundColor(Color.LTGRAY);
        mrunning = true;
    }
    
    public void onStopClick(View view)
    {
    	mPlayer.pause();
        TextView compl = (TextView) findViewById(R.id.completion);
        compl.setText("not runnin'");
        mrunning = false;
        View modifyButton = (View)findViewById(R.id.button2);
        modifyButton.setBackgroundColor(Color.LTGRAY);
        
        View modifyButtont = (View)findViewById(R.id.button1);
        modifyButtont.setBackgroundColor(Color.GREEN);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
    }
    @SuppressLint("SdCardPath")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent)  {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            x = (double)sensorEvent.values[0];
            y = (double)sensorEvent.values[1];
            z = (double)sensorEvent.values[2];
            timeS1 = sensorEvent.timestamp;
            
            long tic = timeS1-timeS2;
            //this is
            if (mrunning && tic >= 10000000) {
                timeS2 = timeS1;
                tvx.setText("X = "+ String.valueOf(x));
                tvy.setText("Y = "+ String.valueOf(y));
                tvz.setText("Z = "+ String.valueOf(z));
                int a = (int) (1000000000/((double)tic));
                tvh.setText("Hz = "+ String.valueOf(a));
                arrayWindowx[windowTic] = x;
                arrayWindowy[windowTic] = y;
                arrayWindowz[windowTic] = z;
                arrayWindowx2[windowTic2] = x;
                arrayWindowy2[windowTic2] = y;
                arrayWindowz2[windowTic2] = z;
                windowTic ++;
                windowTic2 ++;
            }
            //Condition that the sample window is filled
            if (windowTic == 256) {

                //this is just to output how many total approximations there has been
                numOfWin ++;
                numOfCompetions.setText(String.valueOf(numOfWin));

                windowTic = 0;
                //Here we calculate the auto correlation of each accelerometer axis data set.
                //this is a preparation step for finding the energy spectral density of each axis
                double [] autoX = bruteForceAutoCorrelation(arrayWindowx);
                double [] autoY = bruteForceAutoCorrelation(arrayWindowy);
                double [] autoZ = bruteForceAutoCorrelation(arrayWindowz);

                //This replaces each auto correlation with the DFT (discrete fourier transform)
                // of the auto correlation , thus giving us the spectral density.
                //See https://en.wikipedia.org/wiki/Spectral_density for more detail
                fft.realForward(autoX); // autoX now contains the fft
                fft.realForward(autoY);
                fft.realForward(autoZ);
                //below we must convert the fft output to a magnitude to get energy of the spectral density
                double[] ESDx = magnitudeSpectrumConverter(autoX);
                double[] ESDy = magnitudeSpectrumConverter(autoY);
                double[] ESDz = magnitudeSpectrumConverter(autoZ);

                //this calculates the ESD spread for 90% of the energy
                double[] cumsumx = cumsum(ESDx);
                double[] cumsumy = cumsum(ESDy);
                double[] cumsumz = cumsum(ESDz);
                double energySpreadx = nintySpread(cumsumx);
                double energySpready = nintySpread(cumsumy);
                double energySpreadz = nintySpread(cumsumz);

                //This calculates which bin of "truth" values are closest to our three ESD spreads
                double[] err = new double[3];
                err[0] = Math.sqrt(
		                (legTruth[0] - energySpreadx)*(legTruth[0] - energySpreadx)
		                + (legTruth [1] - energySpready)*(legTruth [1] - energySpready)
		                + (legTruth[2] - energySpreadz)*(legTruth[2] - energySpreadz));
                err[1] = Math.sqrt(
                        (chestTruth[0] - energySpreadx) *(chestTruth[0] - energySpreadx)
                                + (chestTruth[1] - energySpready)*(chestTruth[1] - energySpready)
                                + (chestTruth[2] - energySpreadz)*(chestTruth[2] - energySpreadz));
                err[2] = Math.sqrt(
                        (armTruth[0] - energySpreadx)*(armTruth[0]   - energySpreadx)
                                + (armTruth[1] - energySpready) *(armTruth[1] - energySpready)
                                + (armTruth[2] - energySpreadz)*(armTruth[2] - energySpreadz));
                String spot = location[0];
                double min1 = err[0];
                for (int id = 1; id < 3; id++){
                    if (err[id] < min1){
                        min1 = err[id];
                        spot = location[id];
                    }
                }
                //leg
	            if (spot == location[0])
	            	mPlayer2 = MediaPlayer.create(this, R.raw.leg);
                //chest
	            else if (spot == location[1])
	            	mPlayer2 = MediaPlayer.create(this, R.raw.abs);
                //arm
	            else
	            	mPlayer2 = MediaPlayer.create(this, R.raw.arm);

            mPlayer2.start();
            }
        }
    }

    public void CreateFile(String path)    {
        File f = new File(path);
        try {
            Log.d("ACTIVITY", "Create a File.");
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String ReadFile (String filepath)    {
        mBufferedReader = null;
        File file = new File(filepath);
        String tmp = null;
        if (!FileIsExist(filepath)){
        CreateFile(filepath);}
        else{
            file.delete();
            CreateFile(filepath);
        }
        try         {
            mBufferedReader = new BufferedReader(new FileReader(filepath));
            // Read string
            while ((tmp = mBufferedReader.readLine()) != null)             {
                tmp += "\n";
                read_str += tmp;
            }
        }
        catch (IOException e)         {
            e.printStackTrace();
        }
        return read_str;
    }
    public void WriteFile(String filepath, String str)    {
        mBufferedWriter = null;
        if (!FileIsExist(filepath))
            CreateFile(filepath);
        try         {
            mBufferedWriter = new BufferedWriter(new FileWriter(filepath, true));
            mBufferedWriter.write(str);
            mBufferedWriter.newLine();
            mBufferedWriter.flush();
            mBufferedWriter.close();
        }
        catch (IOException e)         {
            e.printStackTrace();
        }
    }
    public boolean FileIsExist(String filepath)    {
        File f = new File(filepath);
        if (! f.exists())        {
            Log.e("ACTIVITY", "File does not exist.");
            return false;
        }
        else        {
            return true;
        }
    }
    @Override
    protected void onPause()    {
        mSensorManager.unregisterListener(this);
        //        Toast.makeText(this, "Unregistered accelerometerListener", Toast.LENGTH_LONG).show();
        super.onPause();
    }
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        //        Toast.makeText(this, "Registered accelerometerListener", Toast.LENGTH_LONG).show();
    }
    
    private double[] squared(double [] x) {
        double[] result = new double[x.length];
        for(int i = 0; i < x.length ; i++ )
        result[i] = x[i]*x[i];
        
        return result;
    }
    public static double[] sqrt (double [] array) {
        double[] result = new double[array.length];
        for(int i = 0; i < array.length ; i++ )
        result[i] = Math.sqrt(array[i]);
        
        return result;
    }
    public static double[] cumsum(double [] array){
        double[] x = new double[array.length];
        x[0] = array[0];
        for(int i = 1; i < array.length; i++)
        x[i] = x[i-1] + array[i];
        return x;
    }
    //this function find the point where 90% of the energy is contained in the signal
    public static double nintySpread(double[] array){
        //since we are looking at the cumulative sum, the last index will represent the total energy
        //of the signal
        double totalEnergy = array[array.length - 1];
        double spread = 0;
        //iterate till we get 90% or more of the total energy
        for(int i = 0; i < array.length; i++){
            if (array[i] >= totalEnergy*0.9){
                spread = (double)(i+1)/(double)array.length;
                break;
            }
        }
        return spread;
    }
    public double[] bruteForceAutoCorrelation(double [] arr) {
        double[] ac = new double[arr.length*2-1];
        double[] temp = new double[arr.length];
        for (int i=0; i<arr.length; i++) {
            for (int j=0; j<arr.length-i; j++) {
                temp[i] += arr[j] * arr[j+i];
            }
        }
        //here I'm flipping the array and appending it to the begining
        int count_up = 0;
        int last = temp.length-1;
        int count_down =temp.length-1;
        for(int k=0; k<temp.length ; k++){
            ac[last + count_up] = temp[count_up];
            ac[count_up] = temp[count_down];
            count_down--;
            count_up++;
        }
        return ac;
    }
    public double[] minusMean(double[] array){
        double[] x = new double[array.length];
        double arLength = array.length;
        double sum = 0;
        for (int i=0; i < arLength; i++){
            sum += array[i];
            
        }
        double mean = sum/arLength;
        for (int i=0; i < arLength; i++){
            x[i] = array[i]- mean;
            
        }
        
        return x;
    }
    //the jtransform library returs our fft call in this form
    //real = fft[2*i];
    //imaginary = fft[2*i+1];
    //so we must take the magnitude of every two indexes (real and imaginary) to represent the ESD
    public double[] magnitudeSpectrumConverter(double[] array){
        //remove the lag zero component of the auto corr DFT to remove the bias gravity has on the axis
        array[0] = 0;
        array[1] = 0;
        double[] magsqSpec = new double[array.length/2];
        array = squared(array);
        int counter = 0;
        for (int i = 2; i < array.length-1;i+= 2){
            magsqSpec[counter] = Math.sqrt(array[i] + array[i+1]);
            counter ++;
        }
        return magsqSpec;
    }
    public double standardDeviation(double[] array){
        double temp = 0;
        double[] meanless = minusMean(array);
        for (int i = 0; i < meanless.length; i++) {
            double test = meanless[i]*meanless[i];
            temp += test  ;
        }
        return Math.sqrt(temp/(double)meanless.length);
    }
    public String changeFileName(String str){
        String corrected;
        
        corrected = str + "(" + Integer.toString(file_counter) + ").txt";
        
        
        return corrected;
    }
}