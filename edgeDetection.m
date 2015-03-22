%% Basic line following

clear all
close all

try
    load('lineVideo.mat','vid');
    disp('Loaded');

catch
    disp('Could not load');

end

frame = 110;
slices = 30:5:380; %% rows that slices were taken from
coords = zeros(length(slices),2);
regress = zeros(length(slices),2);

b = fir1(64,0.01);
%freqz(b);

for i = 1:length(slices)
   video = imrotate(vid(:,:,frame),90);
   slice = video(slices(i),:);
   flt = filter(b,1,slice-mean(slice));
   flt = flt(1,32:end);
   [Y,I] = max(flt);
   %fprintf('Max at (%d,%d) with value %d\n',I,slices(i),Y);
   coords(i,1) = I;
   coords(i,2) = slices(i);
end

imshow(video);
hold on;
%scatter(coords(:,1),coords(:,2));
%{
for k = 1:length(coords)-1
    distance = sqrt((coords(k,1)-coords(k+1,1))^2 + (coords(k,2)-coords(k+1,2))^2);
    if (distance < 10)
        scatter(coords(k,1),coords(k,2));
    end
end
%}
[r,m,b] = regression(coords,regress);
scatter(regress(:,1),regress(:,2))

