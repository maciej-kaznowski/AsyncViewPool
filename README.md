# AsyncViewPool

A library to asynchronously inflate views ahead of time to use later, with automatic memory management.

![alt text](https://raw.githubusercontent.com/maciej-kaznowski/AsyncViewPool/docs/img/Screenshot_Sample_20190806-180447.png

## Installation

TODO

## Usage

#### Create a pool:
```
val viewPool = AsyncViewPool(context)
```

Or to provide the correct LayoutParams (such as when using in a RecyclerView)
```
val viewPool = AsyncViewPool(viewGroup)
```

#### Inflate some views ahead of time:
```
//inflates 1x R.layout.my_layout
viewPool.inflate(R.layout.my_layout)

//inflates 10x R.layout.my_layout
viewPool.inflate(R.layout.my_layout, 10) 

//inflate several layouts once
viewPool.inflate(
    listOf(R.layout.my_layout, R.layout.my_layout_2)
)

//inflate using a parent (for example a RecyclerView), defaults to the ViewGroup provided in the constructor
viewPool.inflate(R.layout.my_layout, parent)
```

#### Retrieve the View when you need it:
```
val view: View? = viewPool.getView(R.layout.my_layout)
```

or if you want to inflate one immediately on the UI thread when one isn't available:
```
val view: View = viewPool.getViewOrInflate(R.layout.my_layout)
```

#### Destroy the AsyncViewPool when finished:
```
override fun onDestroy() {
    super.onDestroy()
    //very important that we destroy the ViewPool once we no longer need it. Failing to do so will result in a memory leak
    viewPool?.destroy()
}

```

<i>A full example is in the sample app</i>

## Debugging
Not all layouts can be inflated asynchronously. Layouts which contain a `fragment` tag and views which create a `Handler` or access `Looper.myLooper()` will fail and instead resort to inflating on the UI thread. To identify these cases, look at the logcat for the following message:

> Failed to inflate resource in the background! Retrying on the UI thread
android.view.InflateException: Binary XML file line #6: Binary XML file line #6: Error inflating class android.webkit.WebView
Caused by: android.view.InflateException: Binary XML file line #6: Error inflating class android.webkit.WebView

You can also enable `setDebug(true)` on the AsyncViewPool instance to debug the following:
* When a layout has been requested to be inflated
* When a layout has been removed from the pool
* When a layout has been removed from the pool due to changes in lifecycle/memory