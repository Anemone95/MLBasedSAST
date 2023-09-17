# ML-based-SAST

**ML-based-SAST** 是一个使用程序切片和BLSTM降低污点传播SAST误报率的工具demo。

# Project Structure

```plain
.
├── README.md
├── ml # 机器学习模块
│   ├── _theano # BLSTM theano实现版本
│   ├── api.py # API Server
│   ├── console.py # CLI
│   ├── data # 学习用的知识库（包括slice和label）
│   ├── model # BLSTM model
│   ├── preprocessing.py # 数据预处理，包括tokenize
│   ├── settings.py
│   ├── tests # Test case
│   ├── tf # BLSTM tensorflow实现版本
│   └── utils # 用于转换格式
└── report2slice # 切词模块
    ├── slice # 生成切片文件
    ├── core # 核心切片和预测模块
    ├── cli # 切片/预测控制台入口
    ├── spotbugsGUI # 改造版spotbugsGUI
    └── pom.xml
```

# Build

1. ML-based-SAST 依赖于改造后的Joana，因此第一步是使用如下命令构建joana：

    ```bash
    # Fetch sources
    git clone https://github.com/anemone95/joana-mvn
    cd joana
    mvn clean install -DskipTests
    ```
    
2. 构建切片和预测模块

   ```bash
   # Fetch sources
   git clone https://github.com/Anemone95/MLBasedSAST
   cd MLBasedSAST/report2slice
   mvn clean package
   ```

3. 安装学习模块环境，学习模块依赖如下库

    ```plain
    tensorflow==2.0.0
    requests==2.22.0
    flask==1.1.1
    theano==1.0.4
    fire==0.2.1
    ```

# Usage

## API.py——预测服务器

启动一个服务器用来预测，接受slice和label，以及启动一个训练

```bash
cd MLBasedSAST/ml
python api.py --model-npz=xxx.npz # run api server
```

## Spotbugs GUI

```bash
java -jar report2slice/spotbugsGUI/target/spotbugsGUI-1.0-SNAPSHOT.jar
```

启动后可以看到一个修改版的Spotbugs GUI，首先新建/打开一个project，获取分析结果，该步骤与原版操作过程相同：

![image-20191121143710061](README/image-20191121143710061.png)

### 设置服务器

点击"AI->Set Server"，设置用于预测的服务器:

![image-20191121143846462](README/image-20191121143846462.png)

### 切片并获取预测结果

点击"AI->Slice and Predict"，程序会首先分析污点传播结果，对相关Bug进行切片：

![image-20191121160401714](README/image-20191121160401714.png)

切片完成后，程序发送给服务器获取预测结果，可以在左侧看到预测结果。

### 清空数据

如果在分析过程中中断，再次slice时会从失败前的最有一步开始，若想重新开始则可以点击“AI->Clean”清除先前数据。

### 标记数据

不论是否进行预测，都可以对一个漏洞实例进行标记（但需要先切片），右键漏洞实例即可，标记结果会同时发送给服务器，作为将来学习使用

![image-20191121161750306](README/image-20191121161750306.png)

## 命令行入口

命令行入口以Spotbugs的xml报告文件作为输入，以json格式输出切片/预测结果。

### 只做切片

```bash
java -jar report2slice/cli/target/cli-1.0-SNAPSHOT.jar slice -f java-sec-code-1.0.0-spotbugs.xml # 默认切片保存到./slice/{project}文件夹下，可用--output-dir指定输出目录
```

### 切片后预测

```bash
java -jar report2slice/cli/target/cli-1.0-SNAPSHOT.jar slice -f java-sec-code-1.0.0-spotbugs.xml --server http://127.0.0.1:8888/ # 指定预测用服务器，默认预测结果保存到./predict，可用--output指定输出目录
```

## console.py——学习控制台

### 启动一次学习

```bash
cd ml
python console.py train --slice-dir=data/slice/benchmark1.2 --label-dir=data/label/benchmark1.2 --epochs=20 # 切片数据文件夹，标记数据文件夹，最大迭代次数
```

