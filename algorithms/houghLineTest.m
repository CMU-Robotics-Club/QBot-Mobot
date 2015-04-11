%%Hough line tests

clear all
close all

imgs = {'data/IMG_20150409_163120400.jpg', 'data/IMG_20150409_163133653.jpg', 'data/IMG_20150409_163140509.jpg', 'data/IMG_20150409_163154676.jpg'}

for i = 1:length(imgs)
    I = rgb2gray(im2double(imread(imgs{i})));
    threshPick(I);
end