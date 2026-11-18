package com.spectrumai.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class MainActivity extends Activity {
    private final int BG=0xff080d18, PANEL=0xff111c2e, PANEL2=0xff17253b, CYAN=0xff43d9ff, MUTED=0xff9aabc4;
    private LinearLayout root, results, modeRow, modeRow2;
    private EditText input;
    private TextView status;
    private String mode="AUTO";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(20),dp(22),dp(20),dp(30)); root.setBackgroundColor(BG);
        scroll.addView(root);
        LinearLayout brand=new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL); brand.setOrientation(0);
        ImageView logo=new ImageView(this); logo.setImageResource(com.spectrumai.app.R.drawable.ic_spectrum); brand.addView(logo,new LinearLayout.LayoutParams(dp(58),dp(58)));
        LinearLayout names=new LinearLayout(this); names.setOrientation(1); names.setPadding(dp(12),0,0,0);
        TextView title=label("SPECTRUM AI",25,Color.WHITE,true); names.addView(title);
        names.addView(label("OPEN SOURCE INTELLIGENCE",10,CYAN,true)); brand.addView(names,new LinearLayout.LayoutParams(-2,-2));
        root.addView(brand);
        TextView intro=label("Техническая разведка по открытым источникам",13,MUTED,false); LinearLayout.LayoutParams ip=params(-1,-2,16); root.addView(intro,ip);
        TextView sec=label("Выбери тип проверки",14,Color.WHITE,true); root.addView(sec,params(-1,-2,22));
        modeRow=new LinearLayout(this); modeRow.setOrientation(0); root.addView(modeRow,params(-1,-2,10));
        addMode("АВТО","AUTO",modeRow); addMode("ДОМЕН","DOMAIN",modeRow); addMode("IP","IP",modeRow);
        modeRow2=new LinearLayout(this); modeRow2.setOrientation(0); root.addView(modeRow2,params(-1,-2,6));
        addMode("URL","URL",modeRow2); addMode("ТЕЛЕФОН","PHONE",modeRow2); addMode("EMAIL","EMAIL",modeRow2); addMode("TELEGRAM","TELEGRAM",modeRow2);
        input=new EditText(this); input.setSingleLine(true); input.setTextColor(Color.WHITE); input.setHintTextColor(0xff71839e);
        input.setTextSize(15); input.setPadding(dp(15),0,dp(15),0); input.setHint(modeHint());
        input.setBackground(round(PANEL2,dp(13))); root.addView(input,params(-1,dp(54),14));
        Button run=button("ЗАПУСТИТЬ АНАЛИЗ"); root.addView(run,params(-1,dp(52),12));
        run.setOnClickListener(v->startScan());
        status=label("ГОТОВ К ПРОВЕРКЕ",11,CYAN,true); root.addView(status,params(-1,-2,18));
        results=new LinearLayout(this); results.setOrientation(1); root.addView(results,params(-1,-2,10));
        TextView footer=label("Источники: RDAP · Google DNS · crt.sh · ipwho.is · urlscan.io\nТелефон проверяется локально, Telegram — только публичная ссылка.",11,MUTED,false);
        root.addView(footer,params(-1,-2,24));
        setContentView(scroll);
    }

    private void addMode(String text,String value,LinearLayout row) {
        TextView t=label(text,11,value.equals(mode)?BG:MUTED,true); t.setGravity(Gravity.CENTER); t.setTag(value);
        t.setPadding(dp(6),dp(13),dp(6),dp(13)); t.setBackground(round(value.equals(mode)?CYAN:PANEL,dp(10)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1); p.setMargins(dp(3),0,dp(3),0); row.addView(t,p);
        t.setOnClickListener(v->{mode=value; refreshModes(); input.setError(null); input.setHint(modeHint());});
    }
    private String modeHint(){switch(mode){case "AUTO":return "Номер, @username, email, IP, URL или домен";case "IP":return "8.8.8.8";case "URL":return "https://example.com/path";case "PHONE":return "+358...";case "EMAIL":return "you@example.com";case "TELEGRAM":return "@username";default:return "example.com";}}
    private void refreshModes(){for(LinearLayout row:new LinearLayout[]{modeRow,modeRow2})for(int i=0;i<row.getChildCount();i++){TextView x=(TextView)row.getChildAt(i);boolean selected=mode.equals(x.getTag());x.setBackground(round(selected?CYAN:PANEL,dp(10)));x.setTextColor(selected?BG:MUTED);}}

    private void startScan() {
        String raw=input.getText().toString().trim();
        if(raw.isEmpty()){input.setError("Введи значение для проверки");return;}
        final String selectedMode=mode.equals("AUTO")?detectMode(raw):mode;
        if(selectedMode==null){input.setError("Не удалось определить тип. Выбери модуль вручную.");return;}
        String target=raw;
        if(selectedMode.equals("PHONE")){checkPhone(raw);return;}
        if(selectedMode.equals("EMAIL")){checkEmail(raw);return;}
        if(selectedMode.equals("TELEGRAM")){checkTelegram(raw);return;}
        if(selectedMode.equals("URL")) {
            try { if(!raw.matches("(?i)^https?://.*")) raw="https://"+raw; URL parsed=new URL(raw); target=parsed.getHost(); if(target.isEmpty()) throw new Exception(); }
            catch(Exception e){input.setError("Проверь URL");return;}
        }
        if(selectedMode.equals("DOMAIN")) {
            target=target.replaceFirst("(?i)^https?://","").split("/")[0].split(":")[0].toLowerCase();
            if(!target.matches("(?i)^[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9])?\\.[a-z]{2,}$")){input.setError("Введи корректный домен");return;}
        }
        if(selectedMode.equals("IP") && !isIpAddress(target)){input.setError("Введи корректный IPv4 или IPv6");return;}
        final String q=target, original=raw, scanMode=selectedMode;
        results.removeAllViews(); status.setText("АНАЛИЗ ВЫПОЛНЯЕТСЯ…");
        addCard("ЦЕЛЬ",q,"Проверка через открытые технические источники");
        worker.execute(()->{
            if(scanMode.equals("IP")) scanIp(q); else scanDomain(q, original);
        });
    }

    private String detectMode(String raw) {
        String low=raw.toLowerCase();
        if(low.startsWith("@")||low.startsWith("https://t.me/")||low.startsWith("http://t.me/")) return "TELEGRAM";
        if(android.util.Patterns.EMAIL_ADDRESS.matcher(raw).matches()) return "EMAIL";
        if(raw.matches("(?i)^[0-9a-f:.]{3,}$") && raw.contains(":") || raw.matches("^[0-9]{1,3}(\\.[0-9]{1,3}){3}$")) return "IP";
        String digits=raw.replaceAll("[^0-9]","");
        if(raw.matches("^\\+?[0-9() .-]{7,}$") && digits.length()>=7 && digits.length()<=15) return "PHONE";
        if(low.startsWith("http://")||low.startsWith("https://")) return "URL";
        if(raw.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}(/.*)?$")) return "DOMAIN";
        return null;
    }

    private boolean isIpAddress(String value) {
        try {
            java.net.InetAddress address=java.net.InetAddress.getByName(value);
            return address instanceof java.net.Inet4Address || (address instanceof java.net.Inet6Address && value.contains(":"));
        } catch(Exception e) { return false; }
    }

    private void checkPhone(String raw){
        try{
            PhoneNumberUtil util=PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber n=util.parse(raw,"ZZ");
            if(!util.isValidNumber(n)){input.setError("Номер не прошёл проверку формата");return;}
            String region=util.getRegionCodeForNumber(n);
            String type=util.getNumberType(n).toString();
            results.removeAllViews(); status.setText("ЛОКАЛЬНАЯ ПРОВЕРКА НОМЕРА");
            addCard("НОРМАЛИЗОВАННЫЙ НОМЕР",util.format(n,PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL),"https://www.itu.int/");
            addCard("МЕТАДАННЫЕ ФОРМАТА","Регион телефонного плана: "+region+"\nТип диапазона: "+type+"\nФормат: корректный",null);
            addCard("ВАЖНО","Эти сведения не устанавливают владельца номера, его адрес или аккаунты. Поиск частных лиц по номеру не выполняется.",null);
            status.setText("ПРОВЕРКА ЗАВЕРШЕНА");
        }catch(Exception e){input.setError("Укажи номер с кодом страны, например +358...");}
    }
    private void checkEmail(String raw){
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(raw).matches()){input.setError("Проверь формат email");return;}
        String domain=raw.substring(raw.lastIndexOf("@")+1).toLowerCase();
        results.removeAllViews();status.setText("ПРОВЕРКА EMAIL-ДОМЕНА");addCard("АДРЕС",raw,null);
        worker.execute(()->{
            JSONObject mx=getJson("https://dns.google/resolve?name="+enc(domain)+"&type=MX");
            StringBuilder b=new StringBuilder();
            if(mx!=null){JSONArray a=mx.optJSONArray("Answer");if(a!=null)for(int i=0;i<a.length();i++)b.append(a.optJSONObject(i).optString("data")).append("\n");}
            addResult("ПОЧТОВЫЙ ДОМЕН / MX",b.length()==0?"MX-записи не найдены или сервис не ответил.":b.toString(),"https://dns.google/resolve?name="+enc(domain)+"&type=MX");
            addResult("ПРИВАТНОСТЬ","Проверяется только домен почты. Пароли, закрытые утечки и личность владельца не проверяются.",null);
            completeScan();
        });
    }
    private void checkTelegram(String raw){
        String u=raw.trim();if(u.startsWith("https://t.me/"))u=u.substring(13);if(u.startsWith("@"))u=u.substring(1);
        if(!u.matches("[A-Za-z0-9_]{5,32}")){input.setError("Нужен публичный Telegram username");return;}
        String link="https://t.me/"+u;results.removeAllViews();status.setText("ПУБЛИЧНЫЙ ПРОФИЛЬ");
        addCard("TELEGRAM USERNAME","@"+u+"\nСформирована ссылка на публичный профиль или канал. Скрытые данные не извлекаются.",link);
        addCard("ПРОВЕРКА","Проверь публичный аккаунт самостоятельно. Username может измениться или принадлежать другому пользователю.",null);
        status.setText("ССЫЛКА ГОТОВА");
    }

    private void scanDomain(String domain,String original) {
        addResult("RDAP / WHOIS", "Запрос сведений о регистрации домена…", null);
        JSONObject rdap=getJson("https://rdap.org/domain/"+enc(domain));
        if(rdap!=null) {
            String registrar="Не указан"; String created="Не указана"; String expires="Не указана"; String state="Не указан";
            try { JSONArray ev=rdap.optJSONArray("events"); if(ev!=null) for(int i=0;i<ev.length();i++){JSONObject e=ev.optJSONObject(i);String a=e.optString("eventAction");if(a.contains("registration"))created=e.optString("eventDate");if(a.contains("expiration"))expires=e.optString("eventDate");} }catch(Exception ignored){}
            try { JSONArray ns=rdap.optJSONArray("nameservers"); if(ns!=null&&ns.length()>0) state=ns.length()+" DNS-сервер(ов)"; }catch(Exception ignored){}
            try { JSONArray ent=rdap.optJSONArray("entities"); if(ent!=null) for(int i=0;i<ent.length();i++){JSONArray roles=ent.optJSONObject(i).optJSONArray("roles"); if(roles!=null&&roles.toString().contains("registrar")){registrar=ent.optJSONObject(i).optString("handle","Данные доступны");break;}}}catch(Exception ignored){}
            addResult("RDAP / WHOIS","Регистратор: "+registrar+"\nСоздан: "+created+"\nИстекает: "+expires+"\nDNS: "+state,"https://rdap.org/domain/"+enc(domain));
        } else addResult("RDAP / WHOIS","Сервис не вернул данные. Возможно, домен не поддерживается или временно недоступен.", "https://rdap.org/domain/"+enc(domain));
        for(String type:new String[]{"A","AAAA","MX","NS","TXT","CAA","SOA"}) {
            JSONObject dns=getJson("https://dns.google/resolve?name="+enc(domain)+"&type="+type);
            StringBuilder value=new StringBuilder();
            if(dns!=null){JSONArray ans=dns.optJSONArray("Answer");if(ans!=null)for(int i=0;i<ans.length();i++){JSONObject a=ans.optJSONObject(i);if(value.length()>0)value.append("\n");value.append(a.optString("data"));}}
            addResult("DNS "+type,value.length()==0?"Записи не найдены или ответ пустой.":value.toString(),"https://dns.google/resolve?name="+enc(domain)+"&type="+type);
        }
        String certUrl="https://crt.sh/?q=%25."+enc(domain)+"&output=json";
        String certs=getText(certUrl);
        if(certs!=null) try {JSONArray arr=new JSONArray(certs);StringBuilder b=new StringBuilder("Найдено записей сертификатов: "+arr.length());for(int i=0;i<Math.min(arr.length(),8);i++){JSONObject c=arr.optJSONObject(i);b.append("\n• ").append(c.optString("name_value","")).append(" — ").append(c.optString("not_before",""));}addResult("SSL / CERTIFICATE TRANSPARENCY",b.toString(),certUrl);}catch(Exception e){addResult("SSL / CERTIFICATE TRANSPARENCY","Не удалось разобрать ответ сервиса.",certUrl);}
        else addResult("SSL / CERTIFICATE TRANSPARENCY","Сервис временно недоступен.",certUrl);
        String scanUrl="https://urlscan.io/api/v1/search/?q=domain:"+enc(domain);
        JSONObject scan=getJson(scanUrl);
        if(scan!=null){JSONArray arr=scan.optJSONArray("results");StringBuilder b=new StringBuilder("Публичных сканов: "+(arr==null?0:arr.length()));if(arr!=null)for(int i=0;i<Math.min(arr.length(),5);i++){JSONObject x=arr.optJSONObject(i);JSONObject page=x.optJSONObject("page");b.append("\n• ").append(page==null?x.optString("_id"):page.optString("url")).append("\n  ").append(x.optString("_id"));}addResult("URLSCAN.IO",""+b,scanUrl);}else addResult("URLSCAN.IO","Публичный поиск сканов недоступен или вернул ошибку.", "https://urlscan.io/search/#domain:"+enc(domain));
        inspectWebHeaders(domain);
        completeScan();
    }

    private void inspectWebHeaders(String domain) {
        HttpURLConnection c=null;
        try {
            c=(HttpURLConnection)new URL("https://"+domain).openConnection();
            c.setRequestMethod("HEAD"); c.setConnectTimeout(8000); c.setReadTimeout(8000);
            c.setInstanceFollowRedirects(true); c.setRequestProperty("User-Agent","SPECTRUM-AI-Android/1.3");
            int code=c.getResponseCode();
            StringBuilder b=new StringBuilder("HTTPS status: ").append(code);
            String[] headers={"Strict-Transport-Security","Content-Security-Policy","X-Frame-Options","X-Content-Type-Options","Referrer-Policy","Permissions-Policy"};
            for(String h:headers) b.append("\\n").append(h).append(": ").append(c.getHeaderField(h)==null?"не задан":c.getHeaderField(h));
            addResult("WEB SECURITY HEADERS",b.toString(),"https://"+domain);
        } catch(Exception e) {
            addResult("WEB SECURITY HEADERS","Не удалось получить HTTPS-заголовки. Сайт может не поддерживать HTTPS или блокировать HEAD-запрос.","https://"+domain);
        } finally { if(c!=null)c.disconnect(); }
    }

    private void scanIp(String ip) {
        JSONObject info=getJson("https://ipwho.is/"+enc(ip));
        if(info!=null && !info.optBoolean("success",true)){addResult("IP LOOKUP",info.optString("message","Не удалось получить сведения"),"https://ipwho.is/"+enc(ip));}
        else if(info!=null){String text="IP: "+info.optString("ip")+"\nТип: "+info.optString("type")+"\nСтрана: "+info.optString("country")+"\nРегион: "+info.optString("region")+"\nГород: "+info.optString("city")+"\nОрганизация: "+info.optString("connection") ;JSONObject c=info.optJSONObject("connection");if(c!=null)text+="\nПровайдер/ASN: "+c.optString("isp")+" / "+c.optString("asn");addResult("IPWHO.IS",text,"https://ipwho.is/"+enc(ip));}
        else addResult("IPWHO.IS","Сервис не ответил.","https://ipwho.is/"+enc(ip));
        JSONObject rdap=getJson("https://rdap.org/ip/"+enc(ip));
        if(rdap!=null){String v="Сеть: "+rdap.optString("name","не указана")+"\nТип: "+rdap.optString("type","не указан")+"\nHandle: "+rdap.optString("handle","не указан");addResult("IP RDAP",v,"https://rdap.org/ip/"+enc(ip));}
        else addResult("IP RDAP","Данные реестра недоступны.","https://rdap.org/ip/"+enc(ip));
        JSONObject dns=getJson("https://dns.google/resolve?name="+enc(ip)+"&type=PTR");
        addResult("REVERSE DNS",dns==null?"Нет ответа DNS":dns.optJSONArray("Answer")==null?"PTR-запись не найдена":dns.optJSONArray("Answer").toString(),"https://dns.google/");
        completeScan();
    }

    private JSONObject getJson(String address){String s=getText(address);if(s==null)return null;try{return new JSONObject(s);}catch(Exception e){return null;}}
    private String getText(String address){
        HttpURLConnection c=null;
        try{c=(HttpURLConnection)new URL(address).openConnection();c.setRequestMethod("GET");c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setRequestProperty("User-Agent","SPECTRUM-AI-Android/1.3");c.setRequestProperty("Accept","application/json, text/plain, */*");int code=c.getResponseCode();if(code<200||code>=300)return null;BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line);r.close();return b.toString();}catch(Exception e){return null;}finally{if(c!=null)c.disconnect();}
    }
    private void addResult(String heading,String body,String link){ui.post(()->addCard(heading,body,link));}
    private void completeScan(){ui.post(()->status.setText("АНАЛИЗ ЗАВЕРШЁН · ПРОВЕРЬ ИСТОЧНИКИ"));}
    private void addCard(String heading,String body,String link){
        LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(15),dp(13),dp(15),dp(13));card.setBackground(round(PANEL,dp(13)));
        TextView h=label(heading,11,CYAN,true);card.addView(h);
        TextView t=label(body,13,Color.WHITE,false);t.setPadding(0,dp(8),0,0);card.addView(t);
        if(link!=null){TextView l=label("ОТКРЫТЬ ИСТОЧНИК ↗",10,MUTED,true);l.setPadding(0,dp(9),0,0);card.addView(l);card.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(link)));}catch(Exception ignored){}});}
        results.addView(card,params(-1,-2,9));
    }
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(BG);b.setTextSize(13);b.setTypeface(null,Typeface.BOLD);b.setAllCaps(false);b.setBackground(round(CYAN,dp(13)));return b;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    private LinearLayout.LayoutParams params(int w,int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.topMargin=dp(top);return p;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private String enc(String s){try{return URLEncoder.encode(s,"UTF-8");}catch(Exception e){return s;}}
    @Override protected void onDestroy(){super.onDestroy();worker.shutdownNow();}
}