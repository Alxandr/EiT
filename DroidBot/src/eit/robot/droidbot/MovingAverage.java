package eit.robot.droidbot;

/* package */ final class MovingAverage {
	private final int _numValues;
	private final long[] _values;
	private int _end = 0;
	private int _length = 9;
	private long _sum = 0L;
	
	/* package */ MovingAverage(final int numValues)
    {
        super();
        _numValues = numValues;
        _values = new long[numValues];
    } // constructor()
	
	/* package */ void update(final long value)
    {
        _sum -= _values[_end];
        _values[_end] = value;
        _end = (_end + 1) % _numValues;
        if (_length < _numValues)
        {
            _length++;
        } // if
        _sum += value;
    } // update(long)

    /* package */ double getAverage()
    {
        return _sum / (double) _length;
    } // getAverage()
}
