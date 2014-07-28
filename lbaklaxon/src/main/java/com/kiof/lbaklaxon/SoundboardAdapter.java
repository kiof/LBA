package com.kiof.lbaklaxon;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SoundboardAdapter extends ArrayAdapter<String> {
    private final Activity mContext;
    private Resources mResources;
    private TypedArray pictures, pictureSections;
    private String[] textSections, textButtons;

    public SoundboardAdapter(Activity context, String[] names) {
        super(context, R.layout.row, names);

        mContext = context;
        mResources = mContext.getResources();

        pictures = mResources.obtainTypedArray(R.array.pictures);
        pictureSections = mResources.obtainTypedArray(R.array.pictureSections);
        textSections = mResources.getStringArray(R.array.textSections);
        textButtons = mResources.getStringArray(R.array.textButtons);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = mContext.getLayoutInflater();
            convertView = inflater.inflate(R.layout.row, null, true);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.section = (TextView) convertView.findViewById(R.id.section);
            viewHolder.button = (Button) convertView.findViewById(R.id.button);
            viewHolder.animation = AnimationUtils.loadAnimation(mContext, R.anim.zoomin);
            convertView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();
        if (!textSections[position].equals("")) {
            holder.section.setVisibility(View.VISIBLE);
            holder.section.setText(textSections[position]);
            holder.section.setCompoundDrawablesWithIntrinsicBounds(
                    pictureSections.getDrawable(position), null,
                    pictureSections.getDrawable(position), null);
        } else {
            holder.section.setVisibility(LinearLayout.GONE);
        }
        holder.button.setText(textButtons[position]);
        holder.button.setBackgroundDrawable(pictures.getDrawable(position));
//		holder.button.setTag(position);
//		holder.button.playSoundEffect(android.view.SoundEffectConstants.CLICK);
        // Set random animation on buttons
        // animation.setStartOffset(new Random().nextInt(500));
        holder.button.clearAnimation();
        holder.button.startAnimation(holder.animation);
        // Register for long click action
//		mContext.registerForContextMenu(holder.button);

        return convertView;
    }

    static class ViewHolder {
        protected TextView section;
        protected Button button;
        protected Animation animation;
    }
}
