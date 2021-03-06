
#include <stdio.h>
#include <opencv2/opencv.hpp>
#include <windows.h>
#include <time.h>


/*                          This is a windows opencv cpp file exclusively written for controls for fruitninja and duckhunt using red laser .You should manually adjust scrWidth and ScrHeight*/
const int WEB_CAM=1;
const int LAP_CAM=0;
const int MILLI_SECS=100;
enum DETECTION {STARTED,MIDWAY,RELEASED};
enum GAME {DUCK_HUNT,FRUIT_SLICE};

int camWidth,camHeight;
int scrWidth=980,scrHeight=730;
int CAM=LAP_CAM;
int X,Y;
clock_t prev=0,curr=0;
int x_offset=0,y_offset=0;
int game;
bool findPixelAboveThreshold(IplImage* img, int thresh, int* pixelPos);
void LeftMouseClick(int x,int y);
void saveLaserPosition( IplImage* img );


int main()
{

    int sliderValue = 250;
    int brightestPixel [2];
    int detect=RELEASED;
    game=DUCK_HUNT;
    IplImage* tempFrame = NULL;
    CvCapture* capture = cvCaptureFromCAM(CAM);
    IplImage* currentFrame = cvQueryFrame(capture);

    camWidth=currentFrame->width;
    camHeight=currentFrame->height;

    printf(" Camera Resolution: %d X %d",camWidth,camHeight);
      cvNamedWindow("LaserTracking",CV_WINDOW_AUTOSIZE);
     cvNamedWindow("Original",CV_WINDOW_AUTOSIZE);

    cvCreateTrackbar("Threshold","LaserTracking",&sliderValue,250,NULL);

    while(true)
    {

        currentFrame = cvQueryFrame(capture);
        cvFlip(currentFrame,currentFrame,1);

        if(!currentFrame)
        {
            printf("\n No more images");
            break;
        }

        if(!tempFrame)
            tempFrame = cvCreateImage(cvSize(currentFrame->width,currentFrame->height),IPL_DEPTH_8U,currentFrame->nChannels);

        cvCopy(currentFrame,tempFrame);
        cvThreshold(tempFrame,tempFrame,(double)sliderValue,255,CV_THRESH_BINARY);

        if(findPixelAboveThreshold(tempFrame,sliderValue,brightestPixel))
        {
            saveLaserPosition(tempFrame);

            if(detect==RELEASED)
            {
                detect=STARTED;
                printf("\n Started aiming the gun at");
                if(game==FRUIT_SLICE)
                    mouse_event(MOUSEEVENTF_LEFTDOWN,X+x_offset,Y+y_offset,0,0);
                else if(game==DUCK_HUNT)
                    prev=clock();
            }
            else if(detect==STARTED)
            {
                detect=MIDWAY;
                printf("\n Aiming the gun at ");
            }
            switch(game)
            {
            case DUCK_HUNT:
                SetCursorPos(X+x_offset,Y+y_offset);
                printf("\n: X=%d ,Y=%d",X+x_offset,Y+y_offset);
                break;
            case FRUIT_SLICE:
                SetCursorPos(X+x_offset,Y+y_offset);
                printf("\n: X=%d ,Y=%d",X+x_offset,Y+y_offset);
                break;
            }
        }
        else if(detect==STARTED||detect==MIDWAY)
        {
            saveLaserPosition(tempFrame);
            curr=clock();
            switch(game)
            {
            case DUCK_HUNT:
                if(curr-prev>MILLI_SECS)
                {
                    printf("\n Firing at");
                    printf(":X=%d ,Y=%d",X+x_offset,Y+y_offset);
                    SetCursorPos(X+x_offset,Y+y_offset);
                    LeftMouseClick(X+x_offset,Y+y_offset);
                }
                break;
            case FRUIT_SLICE:
                SetCursorPos(X+x_offset,Y+y_offset);
                mouse_event(MOUSEEVENTF_LEFTUP,X+x_offset,Y+y_offset,0,0);
                printf("\n: X=%d ,Y=%d",X+x_offset,Y+y_offset);
                break;
            }
            detect=RELEASED;

        }
        else if(game==FRUIT_SLICE)
            mouse_event(MOUSEEVENTF_LEFTUP,X+x_offset,Y+y_offset,0,0);

        cvShowImage("Original",currentFrame);
        if(cvWaitKey(10) > 0)
            break;
    }

    cvDestroyWindow("LaserTracking");
    cvReleaseCapture(&capture);

    if(tempFrame)
        cvReleaseImage(&tempFrame);

    return 0;
}

void LeftMouseClick(int x,int y)
{
    mouse_event(MOUSEEVENTF_LEFTDOWN,x,y,0,0);
    mouse_event(MOUSEEVENTF_LEFTUP,x,y,0,0);
}

//look for Pixel with value above threshold thresh
bool findPixelAboveThreshold(IplImage* img, int thresh, int* pixelPos)
{
    bool found = false;
    uchar* data = (uchar*)img->imageData;
    int step = img->widthStep;
    int channels = img->nChannels;

    for(int i=0; i<img->height; i++)
    {
        for(int j=0; j<img->width; j++)
        {
            for(int k=0; k<channels; k++)
            {
                int intensity = data[i*step+j*channels+k];
                if(intensity > thresh)
                {
                    pixelPos[1] = i;
                    pixelPos[0] = j;                //    SetCursorPos(i,j);
                    found = true;
                }
            }
        }
    }
    return found;
}

void saveLaserPosition( IplImage* img )
{
    int x,y;
    float x1,y1;
    for(y=0; y < img->height; y++ )
    {
        uchar* ptr = (uchar*) (img->imageData + y * img->widthStep);
        for(x=0; x < img->width; x++ )
        {
            if(ptr[3*x]<20&&ptr[3*x+1]<20&&ptr[3*x+2]>180)
            {
                x1=x;
                if(y<40&&game==FRUIT_SLICE)
                    y=40;
                y1=y;
                x1=(x1/640)*scrWidth;
                y1=(y1/480)*scrHeight;
                X=(int)x1;
                Y=(int)y1;
                break;
            }
        }
    }
}
