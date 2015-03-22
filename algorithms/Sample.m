%% Line following with horizontal samples
clear all
close all

%% Load
SCALE = .5;

xyloObj = VideoReader('My Movie.mp4');

nFrames = xyloObj.NumberofFrames;
vidHeight = xyloObj.Height;
vidWidth = xyloObj.Width;

HeightAdj = vidHeight*SCALE;
WidthAdj = vidWidth*SCALE;

StartPoint = 200;
EndPoint = 210;

%% Load frames


    load('lineVideo.mat', 'vid');
    disp('Loaded');

    vid = zeros(HeightAdj,WidthAdj,nFrames);
    im = zeros(vidHeight,vidWidth,3,'uint8');
    for k = StartPoint : EndPoint
        im = read(xyloObj,k);
        hsvim = rgb2hsv(im2double(im));
        vid(:,:,k) = imresize(hsvim(:,:,1), SCALE);
        if mod(k - StartPoint,20) == 19
            disp('Finished 20 frames.');
        end
    end

    save('lineVideo.mat', 'vid');

%% Process video

for i = StartPoint : EndPoint

    impro = imrotate(vid(:,:,i),90);
    % slice = impro(1,StartPoint:200);
    kpts = randi(HeightAdj*WidthAdj,[1,2000]);

    imshow(impro);

    % Note: the width and height are swapped because 
    % the vedeo is rotated. If the video need not
    % to be rotated, swap them back again
    hold on;
     
      [yo,xo] = ind2sub([WidthAdj,HeightAdj],kpts);
%     scatter(xo,yo);
 
      rkpts = kpts(impro(kpts) > 0.5);
      [y,x] = ind2sub([WidthAdj,HeightAdj],rkpts);
%     scatter(x,y);
     
     
%     % Using linear and quadratic approximation
%     cq = polyfit(y,x,2);
%     c1 = polyfit(y,x,1);
%     
%     ys = 0:1:WidthAdj;
%     xq = polyval(cq,ys);
%     xf = polyval(c1,ys);
%     plot(xf,ys,xq,ys,'r');
    
    %Without using the polyfit.
    esize = length(rkpts);
    dis1 = zeros(esize,2);
    dis2 = zeros(esize,3);
    dis1(:,1) = 1;
    dis2(:,1) = 1;
    
    %Find the index of points
    [yf1,xf1] = ind2sub([WidthAdj,HeightAdj],rkpts);
    [yf2,xf2] = ind2sub([WidthAdj,HeightAdj],rkpts);
    dis1(:,2) = yf1(:);
    dis2(:,2) = yf2(:);
    dis2(:,3) = yf2.^2;
    
    xproj1 = transpose(dis1 * inv(transpose(dis1)*dis1) * transpose(dis1) * transpose(xf1));
    xproj2 = transpose(dis2 * inv(transpose(dis2)*dis2) * transpose(dis2) * transpose(xf2));
    err1 = abs(xf1 - xproj1);
    err2 = abs(xf2 - xproj2);
    sd1 = std(err1);
    sd2 = std(err2);
    
    nkpts1 = rkpts(err1 < 2 * sd1);
    nkpts2 = rkpts(err2 < 2 * sd2);
    [yn1,xn1] = ind2sub([WidthAdj,HeightAdj],nkpts1);
    [yn2,xn2] = ind2sub([WidthAdj,HeightAdj],nkpts2);
    
    cq = polyfit(yn2,xn2,2);
    c1 = polyfit(yn1,xn1,1);
    scatter(xn1,yn1);
    
    ys = 0:1:WidthAdj;
    xq = polyval(cq,ys);
    xf = polyval(c1,ys);
    plot(xf,ys,xq,ys,'r');
    
    hold off;
    pause
end
% http://www.owlnet.rice.edu/~ceng303/manuals/matlab/Mat3.html
% 0:57, 1:12, 3:25
    

