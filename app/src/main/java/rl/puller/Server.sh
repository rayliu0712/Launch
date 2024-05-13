mkdir Pulled

adb pull /sdcard/Android/data/rl.puller/files/pull_list.txt

while IFS="" read -r line; do
    adb pull "$line" Pulled >> pull_result.txt
done < pull_list.txt

adb push pull_result.txt /sdcard/Android/data/rl.puller/files

rm pull_list.txt
rm pull_result.txt
