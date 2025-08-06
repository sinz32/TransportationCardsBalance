package me.sinz.transitcard;

import android.content.Context;
import android.view.Gravity;
import android.widget.TextView;

public class SinZ {
    public static int dip2px(Context ctx, int dips) {
        return (int) Math.ceil(dips * ctx.getResources().getDisplayMetrics().density);
    }

    public static TextView copyright(Context ctx) {
        TextView txt = new TextView(ctx);
        txt.setText("© 2023-2025 SinZ, All rights reserved.");
        txt.setGravity(Gravity.CENTER);
        txt.setTextSize(12);
        int pad = dip2px(ctx, 8);
        txt.setPadding(pad, pad, pad, pad);
        return txt;
    }
}
