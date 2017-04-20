package dcamj2;

import org.bridj.Pointer;

import coremem.ContiguousMemoryInterface;
import coremem.exceptions.FreedException;
import coremem.fragmented.FragmentedMemory;
import coremem.fragmented.FragmentedMemoryInterface;
import coremem.interfaces.SizedInBytes;
import coremem.offheap.OffHeapMemory;
import coremem.rgc.Freeable;

/**
 * Dcam image sequence
 *
 * @author royer
 */
public class DcamImageSequence implements SizedInBytes, Freeable
{

  private static final int cPageAlignment = 4096;

  private final FragmentedMemoryInterface mFragmentedMemory;

  private volatile long mBytesPerPixel, mWidth, mHeight, mDepth;
  private volatile long mTimeStampInNs;

  /**
   * Initialises a Dcam image sequence given the number of bytes per pixel, and
   * image sequence width, height and depth. The memory allocation is handled by
   * this constructor.
   * 
   * @param pDcamDevice
   *          device to use for acquisition, this is used to adjust height and
   *          width
   * 
   * @param pBytesPerPixel
   *          bytes per pixel/voxel
   * @param pWidth
   *          width
   * @param pHeight
   *          height
   * @param pDepth
   *          depth
   */
  public DcamImageSequence(DcamDevice pDcamDevice,
                           final long pBytesPerPixel,
                           final long pWidth,
                           final long pHeight,
                           final long pDepth)
  {
    this(pDcamDevice, pBytesPerPixel, pWidth, pHeight, pDepth, true);
  }

  /**
   * Initialises a Dcam image sequence given the number of bytes per pixel, and
   * image sequence width, height, depth and binning (1,2 or 4). The memory
   * allocation is handled by this constructor.
   * 
   * @param pDcamDevice
   *          device to use for acquisition, this is used to adjust height and
   *          width
   * 
   * @param pBytesPerPixel
   *          bytes per pixel/voxel
   * @param pWidth
   *          width
   * @param pHeight
   *          height
   * @param pDepth
   *          depth
   * @param pFragmented
   *          true-> allocates multiple independent buffers, false -> allocates
   *          a single contiguous buffer
   */
  public DcamImageSequence(DcamDevice pDcamDevice,
                           final long pBytesPerPixel,
                           final long pWidth,
                           final long pHeight,
                           final long pDepth,
                           boolean pFragmented)
  {
    mBytesPerPixel = pBytesPerPixel;
    mWidth =
           pDcamDevice.adjustWidthHeight(pWidth,
                                         4 / pDcamDevice.getBinning());
    mHeight =
            pDcamDevice.adjustWidthHeight(pHeight,
                                          4 / pDcamDevice.getBinning());
    mDepth = pDepth;

    if (pFragmented)
    {
      mFragmentedMemory = new FragmentedMemory();
      for (int i = 0; i < mDepth; i++)
      {
        long lNumberOfBytes = pBytesPerPixel * mWidth * mHeight;
        OffHeapMemory lAllocatedMemory =
                                       OffHeapMemory.allocateAlignedBytes("DcamImageSequence"
                                                                          + i,
                                                                          lNumberOfBytes,
                                                                          cPageAlignment);
        mFragmentedMemory.add(lAllocatedMemory);
      }
    }
    else
    {
      long lNumberOfBytes =
                          pBytesPerPixel * mWidth * mHeight * mDepth;
      OffHeapMemory lAllocatedMemory =
                                     OffHeapMemory.allocateAlignedBytes("DcamImageSequence",
                                                                        lNumberOfBytes,
                                                                        cPageAlignment);
      mFragmentedMemory = FragmentedMemory.split(lAllocatedMemory,
                                                 mDepth);
    }

  }

  /**
   * Instantiates a Dcam image sequence given a fragmented memory object and
   * corresponding number of bytes per pixel, image width, height and depth.
   * 
   * @param pFragmentedMemory
   *          fragmented memory object
   * @param pBytesPerPixel
   *          bytes per pixel
   * @param pWidth
   *          width
   * @param pHeight
   *          height
   * @param pDepth
   *          depth
   */
  public DcamImageSequence(final FragmentedMemoryInterface pFragmentedMemory,
                           final long pBytesPerPixel,
                           final long pWidth,
                           final long pHeight,
                           final long pDepth)
  {
    mBytesPerPixel = pBytesPerPixel;
    mWidth = pWidth;
    mHeight = pHeight;
    mDepth = pDepth;
    mFragmentedMemory = pFragmentedMemory;
  }

  /**
   * Returns the number of bytes per pixel
   * 
   * @return number of bytes per pixel
   */
  public final long getBytesPerPixel()
  {
    return mBytesPerPixel;
  }

  /**
   * Returns this image sequence width
   * 
   * @return image sequence width
   */
  public final long getWidth()
  {
    return mWidth;
  }

  /**
   * Returns this image sequence height
   * 
   * @return image sequence height
   */
  public final long getHeight()
  {
    return mHeight;
  }

  /**
   * Returns this image sequence depth
   * 
   * @return image sequence depth
   */
  public final long getDepth()
  {
    return mDepth;
  }

  /**
   * Sets this image sequence time stamp.
   * 
   * @param pTimeStampInNs
   *          image sequence time stamp.
   */
  public void setTimeStampInNs(final long pTimeStampInNs)
  {
    mTimeStampInNs = pTimeStampInNs;
  }

  /**
   * Returns this image sequence time stamp
   * 
   * @return image sequence time stamp
   */
  public final long getTimeStampInNs()
  {
    return mTimeStampInNs;
  }

  /**
   * Returns a BridJ pointer for the plane of given index
   * 
   * @param pIndex
   *          plane index
   * @return BridK pointer
   */
  public Pointer<Byte> getPointerForPlane(final int pIndex)
  {
    ContiguousMemoryInterface lMemoryForPlane =
                                              getMemoryForPlane(pIndex);
    return lMemoryForPlane.getBridJPointer(Byte.class);
  }

  /**
   * Returns memory for a given plane index
   * 
   * @param pIndex
   *          plane index
   * @return memory object for plane
   */
  public ContiguousMemoryInterface getMemoryForPlane(final int pIndex)
  {
    return mFragmentedMemory.get(pIndex);
  }

  /**
   * Returns a Dcam image sequence for a single image of given index from this
   * image sequence
   * 
   * @param pIndex
   *          image index
   * @return Dcam image sequence
   */
  public DcamImageSequence getSinglePlaneImageSequence(final int pIndex)
  {
    ContiguousMemoryInterface lMemory = getMemoryForPlane(pIndex);
    DcamImageSequence lDcamImageSequence =
                                         new DcamImageSequence(FragmentedMemory.wrap(lMemory),
                                                               getBytesPerPixel(),
                                                               getWidth(),
                                                               getDepth(),
                                                               1);
    return lDcamImageSequence;
  }

  /**
   * Consolidates (copies) the contents of this image sequence into a
   * 
   * @param pDestinationMemory
   *          destination memory
   */
  public void consolidateTo(final ContiguousMemoryInterface pDestinationMemory)
  {
    mFragmentedMemory.makeConsolidatedCopy(pDestinationMemory);
  }

  /**
   * Returns the number of fragments
   * 
   * @return number of fragments
   */
  public long getNumberOfFragments()
  {
    return mFragmentedMemory.getNumberOfFragments();
  }

  @Override
  public long getSizeInBytes()
  {
    return mFragmentedMemory.getSizeInBytes();
  }

  @Override
  public void free()
  {
    mFragmentedMemory.free();
  }

  @Override
  public boolean isFree()
  {
    return mFragmentedMemory.isFree();
  }

  @Override
  public void complainIfFreed() throws FreedException
  {
    mFragmentedMemory.complainIfFreed();
  }

  @Override
  public String toString()
  {
    return String.format("DcamImageSequence [mBytesPerPixel=%d, mWidth=%d, mHeight=%d, mDepth=%d, mTimeStampInNs=%d]",
                         mBytesPerPixel,
                         mWidth,
                         mHeight,
                         mDepth,
                         mTimeStampInNs);
  }

}
