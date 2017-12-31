package net.hinyari.gohancountdown;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class CountDownTimer
{
    private GohanCountDown main;
    
    private long rawtime;
    private Instant ntpTimeInstance;
    private NTPUDPClient ntpClient;

    private InetAddress ntpAddress;

    private CountDown countDown;
    
    public CountDownTimer()
    {
        // メインクラスのインスタンス読み込み
        main = GohanCountDown.getInstance();
        try {
            ntpAddress = InetAddress.getByName("ntp.nict.jp");
            countDown = new CountDown(this);
            ntpClient = new NTPUDPClient();
            ntpClient.open();
            main.getLabel_appstatus().setText("正常");
            
            countDown.run();
        } catch (UnknownHostException e) {
            main.getLabel_server().setText("不明なホスト名");
            e.printStackTrace();
            ntpClient.close();
        } catch (SocketException e) {
            main.getLabel_server().setText("接続時にエラー");
            e.printStackTrace();
            ntpClient.close();
        }
    }

    public void reloadDisplay()
    {
        if (ntpClient == null) {
            main.getLabel_appstatus().setText("ntcClientが存在しません。");
            return;
        }

        if (!ntpClient.isOpen()) {
            main.getLabel_server().setText("接続されていません");
            return;
        }
        try {
            TimeInfo timeInfo = ntpClient.getTime(ntpAddress);
            rawtime = timeInfo.getReturnTime();
            ntpTimeInstance = Instant.ofEpochMilli(rawtime);
            // 現在時刻を埋め込む
            LocalDateTime ldt = LocalDateTime.ofInstant(ntpTimeInstance, ZoneId.systemDefault());
            main.getLabel_nowtime().setText(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(ldt));

            // LocalDateTimeを取得する
            Instant firstDayof2017 = LocalDateTime.parse("2017-01-01T00:00:00", DateTimeFormatter.ISO_DATE_TIME)
                    .toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.EPOCH));
            Instant firstDayof2018 = LocalDateTime.parse("2018-01-01T00:00:00", DateTimeFormatter.ISO_DATE_TIME)
                    .toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.EPOCH));
            Instant firstDayof2019 = LocalDateTime.parse("2019-01-01T00:00:00", DateTimeFormatter.ISO_DATE_TIME)
                    .toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.EPOCH));

            // 時刻の差をそれぞれ取得する
            Duration durationunt2018 = Duration.between(ntpTimeInstance, firstDayof2018);
            Duration durationfrom2017 = Duration.between(firstDayof2017, ntpTimeInstance);
            Duration durationfrom2018 = Duration.between(firstDayof2018, ntpTimeInstance);
            Duration durationunt2019 = Duration.between(ntpTimeInstance, firstDayof2019);
            long lunt2018seconds = durationunt2018.getSeconds();

            // JLabelに文字を置く
            main.getLabel_unt2018ss().setText(lunt2018seconds <= 0 ? "HAPPY NEW YEAR" : String.valueOf(lunt2018seconds));

            // 2018年内
            if (lunt2018seconds >= 0) {
                main.getLabel_unt2018h().setText(String.format("%02d", lunt2018seconds / 3600));
                main.getLabel_unt2018m().setText(String.format("%02d", (lunt2018seconds / 60) % 60));
                main.getLabel_unt2018s().setText(String.format("%02d", lunt2018seconds % 60));
                main.getLabel_from2017ss().setText(String.valueOf(durationfrom2017.getSeconds()));
            } else {    // 2018年後
                main.getLabel_unt2018h().setText("00");
                main.getLabel_unt2018m().setText("00");
                main.getLabel_unt2018s().setText("00");
                main.getLabel_yearsfrom().setText("2018年からの経過時間");
                main.getLabel_from2017ss().setText(String.valueOf(durationfrom2018.getSeconds()));
            }
            main.getLabel_unt2019ss().setText(String.valueOf(durationunt2019.getSeconds()));

            main.getLabel_server().setText(ntpAddress.getHostName() + " / " + ntpAddress.getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        

        /* 2017年のときのウンコード
        public String getTime()
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            try {
                // NTPクライアントを作成
                NTPUDPClient ntpClient = new NTPUDPClient();
                ntpClient.open();

                // ntp.nict.jpに接続して時刻の情報を取得する。
                TimeInfo timeInfo = ntpClient.getTime(InetAddress.getByName("ntp.nict.jp"));
                // サーバアドレスを表示

                // 接続を閉じる
                ntpClient.close();

                // Date型にぶち込む
                Date now = new Date(timeInfo.getReturnTime());

                // 型にはめて先頭の4字の西暦を取得
                thisyear = String.valueOf(sdf.format(now).substring(0, 4));
                // 取得した値に1を足して次の年を取得
                nextyear = String.valueOf(Integer.valueOf(thisyear) + 1);

                // Date型にぶち込む
                Date year = sdf.parse(nextyear + "/01/01 00:00:00");
                Date thisyear = sdf.parse(GohanCountDown.thisyear + "/01/01 00:00:00");

                // 時刻の差を求める
                long timeDiff = ((year != null ? year.getTime() : 0) - now.getTime()) / 1000;
                long timeDifffrom = (now.getTime() - (thisyear != null ? thisyear.getTime() : 0)) / 1000;

                String displayDiff = (timeDiff < 0) ? "0" : String.valueOf(timeDiff);
                until_second.setText(displayDiff);
                from_second.setText(String.valueOf(timeDifffrom));
                until_year.setText(nextyear + "年まで 後");
                from_year.setText(GohanCountDown.thisyear + "年から");

                return sdf.format(new Date(timeInfo.getReturnTime()));

            } catch (UnknownHostException e) {
                until_second.setText("不正なホスト名");
                from_second.setText("不正なホスト名");
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        */

    public long getRawtime()
    {
        return rawtime;
    }
    
}

class CountDown extends Thread  {
    
    private Timer timer;
    private CountDownTimer instance;
    
    CountDown(CountDownTimer instance) {
        timer = new Timer();
        this.instance = instance;
    }
    
    @Override
    public void run()
    {
        timer.schedule(timerTask, 0L, 500L);
    }
    
    private TimerTask timerTask = new TimerTask()
    {
        @Override
        public void run()
        {
            instance.reloadDisplay();
        }
    };
}
