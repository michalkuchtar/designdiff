package com.powerapps.designdiff

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class HorizontalGuidesViewHolder(parentView: ViewGroup) : GuidesViewHolder(parentView, Mode.HORIZONTAL) {
    override val firstGuide: View by lazy { parentView.findViewById<View>(R.id.horizontalGuideFirst) }
    override val secondsGuide: View by lazy { parentView.findViewById<View>(R.id.horizontalGuideSecond) }
    override val guidesTextInfo: TextView by lazy { parentView.findViewById<TextView>(R.id.horizontalGuidesInfo) }
}