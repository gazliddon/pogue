# Problem

I need a way to have dynamic resources that look immutable to the code that uses them.

For example there are files on disc that are models.

I can detect when one of these files change with a file watch.

That file eventually ends up as something I render in OpenGL after some processing

And the thing that uses the output of the processing knows nothing about files or that processing.

But I want that output to dynamically change when the file changes.

So!

# So

A file watcher that sends the file to a channel.

The channel is created with a transducer that represents the transform needed

What is output is a object that you can deref with @

The object also creates a go loop that can receive new values

Amd a function set a callback when those values are received?

if I have a cb then the channel could be a watch or anything

```
    (def vao-xform
        (comp
            (map from-transit)          ;; deserialize
            (map to-delayed-vao)        ;; a function that will 
        ))

    (def x (bind-to-file "x.json" vao-xform))


```

## That's very specific

What's generic here? It's a binding to a dynamic event that produces data that gets transformed.

The transformation process is just core.async

The event generator will have to be written but has nothing to do with files, that can be generecised

The final output being used is interesting, it's just a caching? Well not quite.

It's an derefable that can receive it's value from a channel and has a default value?

So something like:

```
    (defn chan-atom 
    ([chan default on-change]
    (let [val (atom default)]
        (do
        (go-loop []
            (let [new-val (<! chan)]
            (on-change new-val @val)
            (reset! val new-val)))

        (reify
            clojure.lang.IDeref
            (deref [_]
            @val)))))

    ([chan default]
    (chan-atom chan default (fn [_ _]))))

```

And channels can already take xforms so this nicely generalised.

What about channels closing? Does it need a destructor? Or is that on-change with a reason for change ( :channel-closed :new-value etc) 

So a function that does:

    event-source -> xform -> chan-atom

Well that's really

    chan -> chan-atom

And I have that above

## Ideas

Well, a change of OGL context is just an event

I need channel that combines OGL context with vao data

or file data?

So if you send a new value to OGL context channel or to file channel you get

```
    {:file-data str
    :gl-context gl-context}
```






