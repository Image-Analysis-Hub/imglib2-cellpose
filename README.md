[![Build Status](https://github.com/Image-Analysis-Hub/imglib2-cellpose/actions/workflows/build.yml/badge.svg)](https://github.com/Image-Analysis-Hub/imglib2-cellpose/actions/workflows/build.yml)

# ImgLib2 Cellpose

<img src="https://camo.githubusercontent.com/eded2b19d541a149b71e23cd40fa4ce2dfd6158146b04687361704a48e084e26/687474703a2f2f7777772e63656c6c706f73652e6f72672f7374617469632f696d616765732f6c6f676f2e706e673f7261773d54727565" width="128" alt="cellpose-logo"> ➡ <img src="https://github.com/imglib/imglib2/blob/master/doc/imglib2-logo-icon.png?raw=true" width="128" alt="imglib2-logo">

Running Cellpose 3 and 4 from Java with [Appose](https://apposed.org/), using [ImgLib2](https://imglib2.net/imglib2/) data structure.

This small library is meant as a go to for Java developers who want to use Cellpose in their projects, without having to worry about the details of how to call Python from Java.
The use of ImgLib2 lightweight data structure allows being agnostic to how images are managed in your final application.
To see an example in Fiji plugin on this library, check the [Cellpose-Appose](https://imagej.net/plugins/fiji-cellpose) plugin.

## Appose and running the library the first time

> [!WARNING]  
> As this library is based on Appose, the first time you run Cellpose 3 or Cellpose 4, a python environment with the requested Cellpose version will be automatically downloaded (via pixi) and installed in your home `.local\shared\appose` directory, which will take some time. 

The environments are managed by [pixi](https://pixi.prefix.dev/latest/), and Appose will download it if you don't have it installed on your system, which also will take some time the first run. So you need to be connected to the internet. 

The next time you use library, the environment will be directly activated and calls will be much faster. So don't panick if not much happens for a while the first time.  

Once installed, the environment will look like this (on Mac, with no NVIDIA GPU):

```zsh
# ~/.local/share/appose/cellpose-appose 
❯ ls -a
.         ..        .pixi     pixi.lock pixi.toml
❯ ls .pixi
envs
❯ ls .pixi/envs
cp3-cpu cp4-cpu
```
On windows and linux, you will see other envs with explicit torch version to support NVIDIA GPUs.
These environments are perfectly fonctional and you can use them separately to run Cellpose 3 and Cellpose 4 from other frameworks. They don't include Cellpose GUI.

Check the `~/.local/share/appose/cellpose-appose/pixi.toml` file for details.

### CUDA-Torch version

The `pixi.toml` current provide environement for `cp3` and `cp4` with torch CPU or CUDA Major.minor version support. They are defined as `<cellpose_version>-<torch_version>`, e.g. `cp3-cpu` for cellpose 3 with CPU or `cp4-cu126` for cellpose 4 with CUDA 12.6.  
You can choose the `torch version` support to use between `cpu`, `cu126` or `cu130` and pass it using the `torchVersion` parameter of the `Cellpose3Parameters` and `Cellpose4Parameters` classes (see below).

More support version can be added by extending the environement defined in the `pixi.toml`.

> [!NOTE]  
> `CUDA` torch support is only available for Windows and Linux system with NVIDIA card.


## Basic usage

Basic usage, e.g. for Cellpose 3:

```java
// Input
final RandomAccessibleInterval< UnsignedByteType > input = ...
// You need to specify the dimensionality of your input
final AxisInfo inputAxes = AxisInfo.XY;

// Get messages about installing and processing, see below
ApposeTaskListener listener = ApposeTaskListener.STD;

// Specify the parameters for Cellpose 3
Cellpose3Parameters params = Cellpose3Parameters.builder()
    .model( Cellpose3BuiltinModels.CYTO2 )
    .channels( 1, 0 ) // 1-based, like Cellpose
    .computeFlows( true ) // flows output won't be null then.
    .build();

// Execute Cellpose 3
CellposeOutput< UnsignedShortType > output = Cellpose.cellpose3( input, inputAxes, params, listener );

// The output.
RandomAccessibleInterval< UnsignedShortType > labels = output.labels;
RandomAccessibleInterval< UnsignedByteType > flows = output.flows;
```
<p>
    <img src="docs/DemoInput.png" alt="Input" width="25%" />
    <img src="docs/DemoOutput.png" alt="Labels output" width="25%" />
    <img src="docs/DemoOuputFlows.png" alt="Flows output" width="25%" />
</p>
(See [basic usage example code](https://github.com/imglib/imglib2-cellpose/blob/main/src/test/java/net/imglib2/cellpose/BasicUsage.java#L62).)

The `Cellpose` gateway class also has a `cellpose4` method, with similar parameters, to run Cellpose 4.

These methods can process any kind of 5D input, from a 'XYCZT' combination of axes. You need to specify what axis is what with the `AxisInfo` record. It has static constant fields for common siutations, that assume the axes order is 'XYCZT'.

> [!WARNING]  
> You can use an input with axes in any order, but the **X and Y axes must be the first two and must be present**. If not an exception will be thrown.

When processing 'XYZT' or 'XYCZT' inputs (5D images), the gateway processes each time-point one after another, because the Python Cellpose does not support 5D inputs. All other cases are batched in one Cellpose call.


## Flows ouput

If in your parameters you set `computeFlows` to `true` (see below), the `CellposeOutput` output will include the flows computed by Cellpose. We chose to simply output the RGB representation of the flows, which is the one shown in the Cellpose GUI, and which is a `RandomAccessibleInterval< UnsignedByteType >` with 3 channels. 

## Output type and number of labels

In the example above, the output labels are of type `UnsignedShortType`, which means that the maximum number of labels is 65,535. If you expect more labels, you can use `UnsignedIntType` instead, which allows for up to 4B labels.
The `Cellpose` gateway has methods to that allows you specyfing the desired output type:

```java
CellposeOutput< UnsignedIntType > output = Cellpose.cellpose3(
    input,
    inputAxes,
    new UnsignedIntType(),
    params,
    listener );
```

(See [output type example code](https://github.com/imglib/imglib2-cellpose/blob/main/src/test/java/net/imglib2/cellpose/BasicUsage.java#L35).)

## Speciyfing parameters

Cellpose parameters are specified in with the `Cellpose3Parameters` and `Cellpose4Parameters` classes, respectively. 
They use a builder pattern, which allows your specifying values in a fluent way.

For Cellpose 3, the parameters match the [API arguments](https://cellpose.readthedocs.io/en/v3.1.1.1/api.html#id0).

```java
Cellpose3Parameters params = Cellpose3Parameters.builder()
        .model(cp_model)
        .customModel(custom_model) // if null, we take the builtin model()
        .diameter(cell_diameter)
        .channels( channels )
        .minSize( min_size )
        .normalize( normalize )
        .resample( resample )
        .cellProbThreshold( cellprob_threshold )
        .flowThreshold( flow_threshold )
        .tileOverlap( tile_overlap )
        .computeFlows( compute_flows )
        .do3D( use3d )
        .stitchThreshold( stitch_threshold )
        .flow3dSmooth( flow3d_smooth )
        .nIter( niter )
        .torchVersion(torchVersion)
        .useGpu( useGPU )
        .build();
```

Cellpose 3 builtin models are available as an enum [`Cellpose3BuiltinModels`](https://github.com/imglib/imglib2-cellpose/blob/main/src/main/java/net/imglib2/cellpose/Cellpose3BuiltinModels.java#L35), which you can use to specify the model you want to use. If you want to use a custom model, you can specify the path to the model with the `customModel` parameter.

For Cellpose 4:

```java
Cellpose4Parameters params = Cellpose4Parameters.builder()
        .customModel(custom_model) // if null we use the cpsam model
        .diameter(cell_diameter)
        .chan0( ch1 ) // what channels in the input to send to Cellpose
        .chan1( ch2 )
        .chan2( ch3 )
        .minSize( min_size )
        .normalize( normalize )
        .resample( resample )
        .cellProbThreshold( cellprob_threshold )
        .flowThreshold( flow_threshold )
        .tileOverlap( tile_overlap )
        .computeFlows( compute_flows )
        .do3D( use3d )
        .stitchThreshold( stitch_threshold )
        .flow3dSmooth( flow3d_smooth )
        .nIter( niter )
        .useGpu(useGPU)
        .torchVersion(torchVersion)
        .useGpu( useGPU )
        .build();
```


## The listener interface

You can grab messages sent during the installation of the environment and the processing of the image by Cellpose by implementing the `ApposeTaskListener` interface. Briefly, the useful methods are:

> The messages sent to this consumer and method are the one related to <b>the execution</b> of the Python task and Cellpose.
```java
Consumer< TaskEvent > taskListener();
void message( String msg );
```

> The messages sent to the followins consumers are related to <b>the downloading, installation and deployment of the Appose environments</b>.
```java
Consumer< String > outputListener();
Consumer< String > errorListener();
ProgressConsumer progressListener();
```

You will find in the interface two default implementations of the listener, one that does nothing (`ApposeTaskListener.VOID`), and one that prints messages to the standard output and error (`ApposeTaskListener.STD`).


Just a gotcha, if you are going to implement your own: 
Pixi _always_ sends a message to the installation error listener, even if there is no error, and if the environment is already installed. This message takes this shape (or variants of it):
```
✔ The cp3-cpu environment has been installed.
```
We are still discussiing whether this is a [bug](https://github.com/apposed/appose/issues/28). In the meantime, it might be nice to your users to catch this message and not display it as an error. 


## Cellpose runner and advanced usage

The `CellposeRunner` class allows for batch processing efficiently.

Running `Cellpose.cellpose3()` involves initializing a pixi environment, creating a Python service, then launching a Python task that loads a Cellpose model, and evaluate it on the input image. 
This is not optimal if you want to process many images, as the initialization and loading phases are long and are repeated for each image.
The `CellposeRunner` class allows you to initialize the environment and load the model once, and then run Cellpose on as many images as you want.

An outline of the usage is the following:

```java
try ( 
    // The tmp data location to pass input to Cellpose.
    ShmImg< UnsignedByteType > tmpInput = Cellpose.createInputShmImg( ... );
    // The tmp data location to receive the Cellpose labels output.
    ShmImg< UnsignedShortType > tmpLabels  = Cellpose.createOutputLabelsShmImg( ... );
    // The tmp data location to receive the Cellpose flows output.
    ShmImg< UnsignedByteType > tmpFlows = Cellpose.createOutputFlowsShmImg( ... );
    // The Cellpose runner, initialized with the tmp data locations.
    // If you pass it a Cellpose 3 parameter object, it will be a
    // runner configured to run Cellpose 3.
    CellposeRunner< UnsignedByteType, UnsignedShortType > runner = Cellpose.cellposeRunner(
            params,
            ApposeTaskListener.VOID,
            tmpInput,
            axes,
            tmpLabels,
            tmpFlows ) )            
{
    runner.init();
    for ( int i = 0; i < nImages; i++ )
	{
        RandomAccessibleInterval< UnsignedByteType > input = ...
        // Copy input to shared data location.
        ImgUtil.copy( input, tmpInput );
        // Run Cellpose. The results will be written in the tmpLabels
        // and tmpFlows images.
        startTime = System.currentTimeMillis();
        runner.run();
        // The output of Cellpose is now in tmpLabels and tmpFlows, you can copy it to your final destination.
    }
}
// Outside of the try-with-resources block, the runner and the tmp shared memory images are
// closed and cleaned up.
```
(See [CellposeRunner example code](https://github.com/imglib/imglib2-cellpose/blob/main/src/test/java/net/imglib2/cellpose/BasicUsage.java#L94).)

Here is an example of the timing on my Macbook Pro M1, processing 10 512x512 8-bit images:
```
Runner and placeholders creation time: 0.34 seconds
Runner initialization time: 2.54 seconds

Processing image 1/10
Input copy time: 0.03 seconds
Cellpose run time: 0.37 seconds
Output copy to a new image time: 0.02 seconds

Processing image 2/10
Input copy time: 0.02 seconds
Cellpose run time: 0.13 seconds
Output copy to a new image time: 0.00 seconds
...
```

## Example parallel processing and tile processing

The file [DemoTileProcessing.java](https://github.com/imglib/imglib2-cellpose/blob/main/src/test/java/net/imglib2/cellpose/tiles/DemoTileProcessing.java) contains a demo on processing a (theoretically) large image by tiles, and processing the tiles in parallel with a pool of `CellposeRunner`. 
Four runners are created, each on a separate thread, and process each a subset of the tiles that cover
the input image. The results are then stitched together to produce the final segmentation of the whole image.

See the results here on a 2560x2160 image from the IDR:
https://bsky.app/profile/jytinevez.bsky.social/post/3mmz45rwxns2l 
