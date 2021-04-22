package com.android.systemui.recents.events;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class EventBus {
    private static final Comparator<EventHandler> EVENT_HANDLER_COMPARATOR = new Comparator<EventHandler>() {
        /* class com.android.systemui.recents.events.EventBus.AnonymousClass1 */

        public int compare(EventHandler eventHandler, EventHandler eventHandler2) {
            int i = eventHandler.priority;
            int i2 = eventHandler2.priority;
            if (i != i2) {
                return i2 - i;
            }
            return Long.compare(eventHandler2.subscriber.registrationTime, eventHandler.subscriber.registrationTime);
        }
    };
    private static volatile EventBus sDefaultBus;
    private static final Object sLock = new Object();
    private HashMap<Class<? extends Event>, ArrayList<EventHandler>> mEventTypeMap = new HashMap<>();
    private Handler mHandler;
    private HashMap<Class<? extends Object>, ArrayList<EventHandlerMethod>> mSubscriberTypeMap = new HashMap<>();
    private ArrayList<Subscriber> mSubscribers = new ArrayList<>();

    public static class Event implements Cloneable {
        boolean cancelled;
        boolean requiresPost;
        boolean trace;

        /* access modifiers changed from: package-private */
        public void onPostDispatch() {
        }

        /* access modifiers changed from: package-private */
        public void onPreDispatch() {
        }

        protected Event() {
        }

        /* access modifiers changed from: protected */
        @Override // java.lang.Object
        public Object clone() throws CloneNotSupportedException {
            Event event = (Event) super.clone();
            event.cancelled = false;
            return event;
        }
    }

    public static class AnimatedEvent extends Event {
        private final ReferenceCountedTrigger mTrigger = new ReferenceCountedTrigger();

        protected AnimatedEvent() {
        }

        public ReferenceCountedTrigger getAnimationTrigger() {
            return this.mTrigger;
        }

        public void addPostAnimationCallback(Runnable runnable) {
            this.mTrigger.addLastDecrementRunnable(runnable);
        }

        /* access modifiers changed from: package-private */
        @Override // com.android.systemui.recents.events.EventBus.Event
        public void onPreDispatch() {
            this.mTrigger.increment();
        }

        /* access modifiers changed from: package-private */
        @Override // com.android.systemui.recents.events.EventBus.Event
        public void onPostDispatch() {
            this.mTrigger.decrement();
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.recents.events.EventBus.Event, java.lang.Object
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }

    public static class ReusableEvent extends Event {
        private int mDispatchCount;

        protected ReusableEvent() {
        }

        /* access modifiers changed from: package-private */
        @Override // com.android.systemui.recents.events.EventBus.Event
        public void onPostDispatch() {
            super.onPostDispatch();
            this.mDispatchCount++;
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.recents.events.EventBus.Event, java.lang.Object
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }

    private EventBus(Looper looper) {
        this.mHandler = new Handler(looper);
    }

    public static EventBus getDefault() {
        if (sDefaultBus == null) {
            synchronized (sLock) {
                if (sDefaultBus == null) {
                    sDefaultBus = new EventBus(Looper.getMainLooper());
                }
            }
        }
        return sDefaultBus;
    }

    public void register(Object obj) {
        registerSubscriber(obj, 1);
    }

    public void register(Object obj, int i) {
        registerSubscriber(obj, i);
    }

    public void unregister(Object obj) {
        ArrayList<EventHandlerMethod> arrayList;
        if (Thread.currentThread().getId() != this.mHandler.getLooper().getThread().getId()) {
            throw new RuntimeException("Can not unregister() a subscriber from a non-main thread.");
        } else if (findRegisteredSubscriber(obj, true) && (arrayList = this.mSubscriberTypeMap.get(obj.getClass())) != null) {
            Iterator<EventHandlerMethod> it = arrayList.iterator();
            while (it.hasNext()) {
                ArrayList<EventHandler> arrayList2 = this.mEventTypeMap.get(it.next().eventType);
                for (int size = arrayList2.size() - 1; size >= 0; size--) {
                    if (arrayList2.get(size).subscriber.getReference() == obj) {
                        arrayList2.remove(size);
                    }
                }
            }
        }
    }

    public void send(Event event) {
        if (Thread.currentThread().getId() == this.mHandler.getLooper().getThread().getId()) {
            event.requiresPost = false;
            event.cancelled = false;
            queueEvent(event);
            return;
        }
        throw new RuntimeException("Can not send() a message from a non-main thread.");
    }

    public void post(Event event) {
        event.requiresPost = true;
        event.cancelled = false;
        queueEvent(event);
    }

    public void sendOntoMainThread(Event event) {
        if (Thread.currentThread().getId() != this.mHandler.getLooper().getThread().getId()) {
            post(event);
        } else {
            send(event);
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.println(dumpInternal(str));
    }

    public String dumpInternal(String str) {
        String str2 = str + "  ";
        String str3 = str2 + "  ";
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("Registered class types:");
        sb.append("\n");
        ArrayList arrayList = new ArrayList(this.mSubscriberTypeMap.keySet());
        Collections.sort(arrayList, new Comparator<Class<?>>() {
            /* class com.android.systemui.recents.events.EventBus.AnonymousClass2 */

            public int compare(Class<?> cls, Class<?> cls2) {
                return cls.getSimpleName().compareTo(cls2.getSimpleName());
            }
        });
        for (int i = 0; i < arrayList.size(); i++) {
            sb.append(str2);
            sb.append(((Class) arrayList.get(i)).getSimpleName());
            sb.append("\n");
        }
        sb.append(str);
        sb.append("Event map:");
        sb.append("\n");
        ArrayList arrayList2 = new ArrayList(this.mEventTypeMap.keySet());
        Collections.sort(arrayList2, new Comparator<Class<?>>() {
            /* class com.android.systemui.recents.events.EventBus.AnonymousClass3 */

            public int compare(Class<?> cls, Class<?> cls2) {
                return cls.getSimpleName().compareTo(cls2.getSimpleName());
            }
        });
        for (int i2 = 0; i2 < arrayList2.size(); i2++) {
            Class cls = (Class) arrayList2.get(i2);
            sb.append(str2);
            sb.append(cls.getSimpleName());
            sb.append(" -> ");
            sb.append("\n");
            Iterator<EventHandler> it = this.mEventTypeMap.get(cls).iterator();
            while (it.hasNext()) {
                EventHandler next = it.next();
                Object reference = next.subscriber.getReference();
                if (reference != null) {
                    String hexString = Integer.toHexString(System.identityHashCode(reference));
                    sb.append(str3);
                    sb.append(reference.getClass().getSimpleName());
                    sb.append(" [0x" + hexString + ", #" + next.priority + "]");
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: java.util.HashMap<java.lang.Class<? extends java.lang.Object>, java.util.ArrayList<com.android.systemui.recents.events.EventHandlerMethod>> */
    /* JADX DEBUG: Multi-variable search result rejected for r8v1, resolved type: java.util.HashMap<java.lang.Class<? extends com.android.systemui.recents.events.EventBus$Event>, java.util.ArrayList<com.android.systemui.recents.events.EventHandler>> */
    /* JADX WARN: Multi-variable type inference failed */
    private void registerSubscriber(Object obj, int i) {
        if (Thread.currentThread().getId() != this.mHandler.getLooper().getThread().getId()) {
            throw new RuntimeException("Can not register() a subscriber from a non-main thread.");
        } else if (!findRegisteredSubscriber(obj, false)) {
            Subscriber subscriber = new Subscriber(obj, SystemClock.uptimeMillis());
            Class<?> cls = obj.getClass();
            ArrayList<EventHandlerMethod> arrayList = this.mSubscriberTypeMap.get(cls);
            if (arrayList != null) {
                Iterator<EventHandlerMethod> it = arrayList.iterator();
                while (it.hasNext()) {
                    EventHandlerMethod next = it.next();
                    ArrayList<EventHandler> arrayList2 = this.mEventTypeMap.get(next.eventType);
                    arrayList2.add(new EventHandler(subscriber, next, i));
                    sortEventHandlersByPriority(arrayList2);
                }
                this.mSubscribers.add(subscriber);
                return;
            }
            ArrayList arrayList3 = new ArrayList();
            this.mSubscriberTypeMap.put(cls, arrayList3);
            this.mSubscribers.add(subscriber);
            Method[] declaredMethods = cls.getDeclaredMethods();
            for (Method method : declaredMethods) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (isValidEventBusHandlerMethod(method, parameterTypes)) {
                    Class<?> cls2 = parameterTypes[0];
                    ArrayList<EventHandler> arrayList4 = this.mEventTypeMap.get(cls2);
                    if (arrayList4 == null) {
                        arrayList4 = new ArrayList<>();
                        this.mEventTypeMap.put(cls2, arrayList4);
                    }
                    EventHandlerMethod eventHandlerMethod = new EventHandlerMethod(method, cls2);
                    arrayList4.add(new EventHandler(subscriber, eventHandlerMethod, i));
                    arrayList3.add(eventHandlerMethod);
                    sortEventHandlersByPriority(arrayList4);
                }
            }
        }
    }

    private void queueEvent(Event event) {
        ArrayList<EventHandler> arrayList = this.mEventTypeMap.get(event.getClass());
        if (arrayList == null) {
            event.onPreDispatch();
            event.onPostDispatch();
            return;
        }
        event.onPreDispatch();
        ArrayList arrayList2 = (ArrayList) arrayList.clone();
        int size = arrayList2.size();
        boolean z = false;
        for (int i = 0; i < size; i++) {
            EventHandler eventHandler = (EventHandler) arrayList2.get(i);
            if (eventHandler.subscriber.getReference() != null) {
                if (event.requiresPost) {
                    this.mHandler.post(new Runnable(eventHandler, event) {
                        /* class com.android.systemui.recents.events.$$Lambda$EventBus$q4Zjc4wBbkxnrUVVPApDejTDoCs */
                        private final /* synthetic */ EventHandler f$1;
                        private final /* synthetic */ EventBus.Event f$2;

                        {
                            this.f$1 = r2;
                            this.f$2 = r3;
                        }

                        public final void run() {
                            EventBus.this.lambda$queueEvent$0$EventBus(this.f$1, this.f$2);
                        }
                    });
                    z = true;
                } else {
                    lambda$queueEvent$0$EventBus(eventHandler, event);
                }
            }
        }
        if (z) {
            Handler handler = this.mHandler;
            Objects.requireNonNull(event);
            handler.post(new Runnable() {
                /* class com.android.systemui.recents.events.$$Lambda$a517Vrmm8lfhf0yP73BQOnPEOkc */

                public final void run() {
                    EventBus.Event.this.onPostDispatch();
                }
            });
            return;
        }
        event.onPostDispatch();
    }

    /* access modifiers changed from: private */
    /* renamed from: processEvent */
    public void lambda$queueEvent$0$EventBus(EventHandler eventHandler, Event event) {
        if (!event.cancelled) {
            try {
                if (event.trace) {
                    logWithPid(" -> " + eventHandler.toString());
                }
                Object reference = eventHandler.subscriber.getReference();
                if (reference != null) {
                    eventHandler.method.invoke(reference, event);
                } else {
                    Log.e("EventBus", "Failed to deliver event to null subscriber");
                }
            } catch (IllegalAccessException e) {
                Log.e("EventBus", "Failed to invoke method", e.getCause());
            } catch (InvocationTargetException e2) {
                throw new RuntimeException(e2.getCause());
            }
        } else if (event.trace) {
            logWithPid("Event dispatch cancelled");
        }
    }

    private boolean findRegisteredSubscriber(Object obj, boolean z) {
        for (int size = this.mSubscribers.size() - 1; size >= 0; size--) {
            if (this.mSubscribers.get(size).getReference() == obj) {
                if (z) {
                    this.mSubscribers.remove(size);
                }
                return true;
            }
        }
        return false;
    }

    private boolean isValidEventBusHandlerMethod(Method method, Class<?>[] clsArr) {
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) || !Modifier.isFinal(modifiers) || !method.getReturnType().equals(Void.TYPE) || clsArr.length != 1 || !Event.class.isAssignableFrom(clsArr[0]) || !method.getName().startsWith("onBusEvent")) {
            return false;
        }
        return true;
    }

    private void sortEventHandlersByPriority(List<EventHandler> list) {
        Collections.sort(list, EVENT_HANDLER_COMPARATOR);
    }

    private static void logWithPid(String str) {
        Log.d("EventBus", "[" + Process.myPid() + ", u" + UserHandle.myUserId() + "] " + str);
    }
}
