package cn.com.wysha.board.ai;

import cn.com.wysha.board.grid_board.AbstractGridBoard;
import cn.com.wysha.board.grid_board.BoardPositionState;
import cn.com.wysha.board.grid_board.Side;
import cn.com.wysha.config_Manager.config.Config;
import cn.com.wysha.config_Manager.config.ConfigField;
import cn.com.wysha.config_Manager.converter.number.IntegerConverter;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Data {
    public final static int MAX_MAP_SIZE = 262144;
    private final ReentrantReadWriteLock readWriteLock;
    private final Map<BoardPositionState, CacheElement> map;
    private final Map<Integer, Set<CacheElement>> freqMap;
    private int minFreq = 0;

    Data(Map<BoardPositionState, CacheElement> map) {
        this.map = map;

        readWriteLock = new ReentrantReadWriteLock();

        freqMap = new HashMap<>();
        map.forEach((key, element) -> {
            element.setKey(key);
            int v = element.getFreq();
            Set<CacheElement> set = freqMap.computeIfAbsent(v, k -> new HashSet<>());
            element.setSet(set);
            if (v < minFreq) {
                minFreq = v;
            }
            set.add(element);
        });
    }

    private void addFreq(CacheElement cacheElement) {
        int tmpFreq = cacheElement.getFreq();
        if (cacheElement.addFreq()) {
            Set<CacheElement> tmpSet = cacheElement.getSet();
            tmpSet.remove(cacheElement);
            Set<CacheElement> set = freqMap.computeIfAbsent(cacheElement.getFreq(), k -> new HashSet<>());
            cacheElement.setSet(set);
            set.add(cacheElement);
            if (tmpFreq == minFreq && tmpSet.isEmpty()) {
                addMinFreq();
            }
        }
    }

    private void addMinFreq() {
        int tmp = minFreq;
        Set<CacheElement> set;
        do {
            set = freqMap.get(++tmp);
        } while (set == null || set.isEmpty());
        minFreq = tmp;
    }

    public DefaultOperation get(BoardPositionState key) {
        CacheElement cacheElement;
        synchronized (readWriteLock.readLock()) {
            cacheElement = map.get(key);
        }
        if (cacheElement == null) {
            return null;
        }

        if (readWriteLock.writeLock().tryLock()) {
            try {
                addFreq(cacheElement);
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }
        return cacheElement.getElement();
    }

    public void put(BoardPositionState key, DefaultOperation element) {
        CacheElement tmp = new CacheElement(element);
        tmp.setKey(key);
        synchronized (readWriteLock.writeLock()) {
            CacheElement cacheElement = map.putIfAbsent(key, tmp);
            if (cacheElement == null) {
                Set<CacheElement> tmp_Set = freqMap.computeIfAbsent(minFreq, k -> new HashSet<>());
                tmp.setSet(tmp_Set);
                tmp_Set.add(tmp);

                if (map.size() >= MAX_MAP_SIZE) {
                    Set<CacheElement> set = freqMap.get(minFreq);
                    Iterator<CacheElement> iterator = set.iterator();
                    CacheElement e = iterator.next();
                    iterator.remove();
                    map.remove(e.getKey());
                }
            } else {
                addFreq(cacheElement);
            }
        }
    }

    public void writeMap(File file) {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file))) {
            objectOutputStream.writeObject(map);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

class CacheElement implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final DefaultOperation element;
    transient ReentrantLock lock = new ReentrantLock();
    private transient BoardPositionState key;
    private transient Set<CacheElement> set;
    private Integer freq = 0;

    CacheElement(DefaultOperation element) {
        this.element = element;
    }

    public BoardPositionState getKey() {
        return key;
    }

    public void setKey(BoardPositionState key) {
        this.key = key;
    }

    public DefaultOperation getElement() {
        return element;
    }

    public Set<CacheElement> getSet() {
        synchronized (lock) {
            return set;
        }
    }

    public void setSet(Set<CacheElement> set) {
        synchronized (lock) {
            this.set = set;
        }
    }

    public Integer getFreq() {
        synchronized (lock) {
            return freq;
        }
    }

    public void setFreq(Integer freq) {
        synchronized (lock) {
            this.freq = freq;
        }
    }

    public boolean addFreq() {
        synchronized (lock) {
            if (freq < Integer.MAX_VALUE) {
                freq++;
                return true;
            } else {
                return false;
            }
        }
    }

    @Serial
    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        lock = new ReentrantLock();
        if (freq > 4) {
            freq /= 2;
            freq = freq < 4 ? 4 : freq;
        } else if (freq > Integer.MIN_VALUE) {
            --freq;
        }
    }
}

/**
 * 通用AI
 */
@Config(parentFilePath = ".\\config\\aiConfig.ini",parentSectionPath = "AI")
public abstract class AbstractAI {
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    /**
     * gameMap = {GameType, sizeMap}
     * sizeMap = {size, heightMap}
     * heightMap = {height, data}
     * data.map = {state, operation}
     */
    protected static final Map<Class<? extends AbstractGridBoard>,
            Map<Integer,
                    Map<Integer, Data>>> gameMap = new HashMap<>();

    @ConfigField
    public static String AI_CACHE_ADDRESS = ".\\aiCache";

    private static final ReadWriteLock gameMapLock = new ReentrantReadWriteLock();
    private static final ReadWriteLock fileLock = new ReentrantReadWriteLock();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(EXECUTOR_SERVICE::shutdown));
    }

    @ConfigField(converter = IntegerConverter.class)
    public static int MIN_SAVE_HEIGHT = 3;

    protected int currentDeepin;
    protected final int maxDeepin;
    protected final int minDeepin;
    private final Map<Integer, Data> heightMap;
    private final AbstractGridBoard gridBoard;
    private final String heightMapURL;

    public AbstractAI(AbstractGridBoard gridBoard, int maxDeepin, int minDeepin) {
        this.gridBoard = gridBoard;
        this.maxDeepin = maxDeepin;
        this.minDeepin = minDeepin;

        Class<? extends AbstractGridBoard> gameType = getGridBoard().getClass();
        int boardSize = gridBoard.getWidth() * gridBoard.getHeight();

        heightMapURL = AI_CACHE_ADDRESS + '\\' + gameType.getName() + '\\' + boardSize + '\\';

        heightMap = gameMap.computeIfAbsent(gameType, k -> new HashMap<>()).computeIfAbsent(boardSize, k -> new HashMap<>());

        LinkedList<Thread> threads = new LinkedList<>();
        for (int i = MIN_SAVE_HEIGHT; i <= maxDeepin; i++) {
            final int index = i;
            Thread thread = new Thread(() -> loadCache(index));
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected AbstractGridBoard getGridBoard() {
        return gridBoard;
    }

    private void loadCache(int height) {
        fileLock.readLock().lock();
        try {
            boolean b;
            gameMapLock.readLock().lock();
            try {
                b = heightMap.containsKey(height);
            } finally {
                gameMapLock.readLock().unlock();
            }

            if (!b) {
                Map<BoardPositionState, CacheElement> map = null;
                File cache = new File(heightMapURL + height);
                if (cache.exists()) {
                    try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(cache))) {
                        map = (HashMap<BoardPositionState, CacheElement>) objectInputStream.readObject();
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

                gameMapLock.writeLock().lock();
                try {
                    heightMap.put(height, new Data(Objects.requireNonNullElseGet(map, HashMap::new)));
                } finally {
                    gameMapLock.writeLock().unlock();
                }
            }
        } finally {
            fileLock.readLock().unlock();
        }
    }

    public final void saveCache() {
        fileLock.writeLock().lock();
        try {
            File root = new File(heightMapURL);
            if (!root.exists()) {
                root.mkdirs();
            }
            LinkedList<Thread> threads = new LinkedList<>();
            heightMap.forEach((key, element) -> {
                Thread thread = new Thread(() -> {
                    File cache = new File(heightMapURL + key);
                    if (!cache.exists()) {
                        try {
                            cache.createNewFile();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    element.writeMap(cache);
                });
                threads.add(thread);
                thread.start();
            });
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    protected final DefaultOperation getOperation(BoardPositionState boardPositionState, int height) {
        return heightMap.get(height).get(boardPositionState);
    }

    protected final void putOperation(BoardPositionState boardPositionState, DefaultOperation defaultOperation) {
        int height = defaultOperation.getHeight();
        heightMap.get(height).put(boardPositionState, defaultOperation);
    }

    /**
     * 获取下一步
     *
     * @return position = {x, y}
     */
    protected abstract DefaultOperation getNext(Side side);

    /**
     * 走下一步,如果下一步获取失败(棋盘已满),则不做任何操作
     */
    public abstract void doNext(Side side);
}
