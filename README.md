# ML-based-SAST

[中文文档](https://github.com/Anemone95/MLBasedSAST/blob/master/README-zh.md)

**ML-based-SAST** is a tool that uses program slicing and BLSTM network to reduce the false positives during taint analysis.


# Project Structure

```plain
.
├── README.md
├── ml # Machine Learning Module
│   ├── _theano # BLSTM implementation in theano
│   ├── api.py # API Server
│   ├── console.py # CLI
│   ├── data # Knowledge base for learning (includes slice and label)
│   ├── model # BLSTM model
│   ├── preprocessing.py # Data preprocessing, including tokenization
│   ├── settings.py
│   ├── tests # Test cases
│   ├── tf # BLSTM implementation in tensorflow
│   └── utils # Used for format conversion
└── report2slice # Slicing module
    ├── slice # Generates slice files
    ├── core # Core slicing and prediction module
    ├── cli # CLI entry for slicing/prediction
    ├── spotbugsGUI # Modified version of spotbugsGUI
    └── pom.xml
```

# Build

1. ML-based-SAST relies on the modified version of Joana. Therefore, the first step is to build joana using the following command:

    ```bash
    # Fetch sources
    git clone https://github.com/anemone95/joana-mvn
    cd joana
    mvn clean install -DskipTests
    ```
    
2. Build the slicing and prediction module:

   ```bash
   # Fetch sources
   git clone https://github.com/Anemone95/MLBasedSAST
   cd MLBasedSAST/report2slice
   mvn clean package
   ```

3. Install the learning module environment. The learning module depends on the following libraries:

    ```plain
    tensorflow==2.0.0
    requests==2.22.0
    flask==1.1.1
    theano==1.0.4
    fire==0.2.1
    ```

# Usage

## API.py - Prediction Server

Start a server for predictions, accepting slice and label, and initiate training:

```bash
cd MLBasedSAST/ml
python api.py --model-npz=xxx.npz # run api server
```

## Spotbugs GUI

```bash
java -jar report2slice/spotbugsGUI/target/spotbugsGUI-1.0-SNAPSHOT.jar
```

After launching, you can see a modified version of the Spotbugs GUI. First, create/open a project and obtain analysis results. This step is similar to the original operation:

![image-20191121143710061](README/image-20191121143710061.png)

### Set Server

Click "AI->Set Server" to set the server for predictions:

![image-20191121143846462](README/image-20191121143846462.png)

### Slice and Get Prediction Results

Click "AI->Slice and Predict". The program will first analyze the taint propagation results and slice the related bugs:

![image-20191121160401714](README/image-20191121160401714.png)

After slicing, the program sends the results to the server for predictions, and you can see the prediction results on the left side.

### Clear Data

If the analysis is interrupted, slicing again will start from the last successful step. If you want to start over, click "AI->Clean" to clear previous data.

### Label Data

Regardless of whether a prediction is made, you can label a vulnerability instance (but slicing is required first). Right-click on the vulnerability instance to do so. The labeling results will be sent to the server for future learning:

![image-20191121161750306](README/image-20191121161750306.png)

## CLI Entry

The CLI entry takes the Spotbugs xml report file as input and outputs the slice/prediction results in json format.

### Slice Only

```bash
java -jar report2slice/cli/target/cli-1.0-SNAPSHOT.jar slice -f java-sec-code-1.0.0-spotbugs.xml # By default, slices are saved to ./slice/{project} folder. Use --output-dir to specify the output directory.
```

### Slice and Predict

```bash
java -jar report2slice/cli/target/cli-1.0-SNAPSHOT.jar slice -f java-sec-code-1.0.0-spotbugs.xml --server http://127.0.0.1:8888/ # Specify the server for predictions. By default, prediction results are saved to ./predict. Use --output to specify the output directory.
```

## console.py - Learning Console

### Start a Learning Session

```bash
cd ml
python console.py train --slice-dir=data/slice/benchmark1.2 --label-dir=data/label/benchmark1.2 --epochs=20 # Slice data folder, label data folder, maximum number of iterations.
```
