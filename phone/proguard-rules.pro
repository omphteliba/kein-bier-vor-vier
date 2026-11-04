# The widget (Glance) runs on WorkManager, whose Room database R8 would
# otherwise strip: the release build crashed at start without these.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
