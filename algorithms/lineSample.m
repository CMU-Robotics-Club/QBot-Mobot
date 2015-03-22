%% Line Sampling

clear all
close all

%% Load

load('frame.mat', 'frame')

%% Slice

sliceLevels = [300];%[100,200,300,400];

figure
hold on
cc=hsv(length(sliceLevels));
i = 1;
for l = sliceLevels
    plot(frame(l,:), 'color', cc(i,:));
    i = i + 1;
end
hold off

%% Filter

CLOSE_TO_ZERO = .01;

N = 64;
b = fir1(N, .1);

S = [1,9];
h1 = fspecial('gaussian', S, .1);
h2 = fspecial('gaussian', S, 1);
h = h1 - h2;

figure
hold on
cc=hsv(length(sliceLevels));
i = 1;
for l = sliceLevels
    flt1 = filter(b,1,frame(l,:));
    flt2 = filter(h,1,flt1) * 100;
    flt2 = flt2(1,N:end);
    % Find zeros
    edges = find(abs(flt2) < CLOSE_TO_ZERO);
    plot(flt1(1,N:end), 'color', cc(i,:));
    plot(abs(flt2), 'color', cc(i,:));
    scatter(edges, zeros(size(edges)));
    i = i + 1;
end
hold off
