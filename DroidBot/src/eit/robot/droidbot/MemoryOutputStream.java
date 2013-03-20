package eit.robot.droidbot;

import java.io.IOException;
import java.io.OutputStream;

/*package*/ final class MemoryOutputStream extends OutputStream {
	private final byte[] _buffer;
	private int _length;
	
	/* package */ MemoryOutputStream(final int size)
    {
        this(new byte[size]);
    } // constructor(int)

    /* package */ MemoryOutputStream(final byte[] buffer)
    {
        super();
        _buffer = buffer;
    } // constructor(byte[])
    
    @Override
    public void write(final byte[] buffer, final int offset, final int count)
            throws IOException
    {
        checkSpace(count);
        System.arraycopy(buffer, offset, _buffer, _length, count);
        _length += count;
    } // write(buffer, offset, count)

    @Override
    public void write(final byte[] buffer) throws IOException
    {
        checkSpace(buffer.length);
        System.arraycopy(buffer, 0, _buffer, _length, buffer.length);
        _length += buffer.length;
    } // write(byte[])

    @Override
    public void write(final int oneByte) throws IOException
    {
        checkSpace(1);
        _buffer[_length++] = (byte) oneByte;
    } // write(int)

    private void checkSpace(final int length) throws IOException
    {
        if (_length + length >= _buffer.length)
        {
            throw new IOException("insufficient space in buffer");
        } // if
    } // checkSpace(int)

    /* package */ void seek(final int index)
    {
        _length = index;
    } // seek(int)

    /* package */ byte[] getBuffer()
    {
        return _buffer;
    } // getBuffer()

    /* package */ int getLength()
    {
        return _length;
    } // getLength()
}
