/*
 * Copyright (c) 2000 jPOS.org. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  3. The end-user documentation included with the redistribution, if any,
 * must include the following acknowledgment: "This product includes software
 * developed by the jPOS project (http://www.jpos.org/)". Alternately, this
 * acknowledgment may appear in the software itself, if and wherever such
 * third-party acknowledgments normally appear.
 *  4. The names "jPOS" and "jPOS.org" must not be used to endorse or promote
 * products derived from this software without prior written permission. For
 * written permission, please contact license@jpos.org.
 *  5. Products derived from this software may not be called "jPOS", nor may
 * "jPOS" appear in their name, without prior written permission of the jPOS
 * project.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE JPOS
 * PROJECT OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the jPOS Project. For more information please see
 * <http://www.jpos.org/> .
 */

package org.jpos.iso;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author joconnor
 * @version $Revision$ $Date$
 */
public class ISOStringFieldPackager extends ISOFieldPackager
{
    private Interpreter interpreter;
    private Padder padder;
    private Prefixer prefixer;

    /**
     * Constructs a default ISOStringFieldPackager. There is no padding,
     * no length prefix and a literal interpretation. The set methods must be called to
     * make this ISOBaseFieldPackager useful.
     */
    public ISOStringFieldPackager()
    {
        super();
        this.padder = NullPadder.INSTANCE;
        this.interpreter = LiteralInterpreter.INSTANCE;
        this.prefixer = NullPrefixer.INSTANCE;
    }

    /**
     * Constructs an ISOStringFieldPackager with a specific Padder, Interpreter and Prefixer.
     * The length and description should be set with setLength() and setDescription methods.
     * @param padder The type of padding used.
     * @param interpreter The interpreter used to encode the field.
     * @param prefixer The type of length prefixer used to encode this field.
     */
    public ISOStringFieldPackager(Padder padder, Interpreter interpreter, Prefixer prefixer)
    {
        super();
        this.padder = padder;
        this.interpreter = interpreter;
        this.prefixer = prefixer;
    }

    /**
     * Creates an ISOStringFieldPackager.
     * @param maxLength The maximum length of the field in characters or bytes depending on the datatype.
     * @param description The description of the field. For human readable output.
     * @param interpreter The interpreter used to encode the field.
     * @param padder The type of padding used.
     * @param prefixer The type of length prefixer used to encode this field.
     */
    public ISOStringFieldPackager(int maxLength, String description, Padder padder,
                                  Interpreter interpreter, Prefixer prefixer)
    {
        super(maxLength, description);
        this.padder = padder;
        this.interpreter = interpreter;
        this.prefixer = prefixer;
    }

    /**
     * Sets the Padder.
     * @param padder The padder to use during packing and unpacking.
     */
    public void setPadder(Padder padder)
    {
        this.padder = padder;
    }

    /**
     * Sets the Interpreter.
     * @param interpreter The interpreter to use in packing and unpacking.
     */
    public void setInterpreter(Interpreter interpreter)
    {
        this.interpreter = interpreter;
    }

    /**
     * Sets the length prefixer.
     * @param prefixer The length prefixer to use during packing and unpacking.
     */
    public void setPrefixer(Prefixer prefixer)
    {
        this.prefixer = prefixer;
    }

    /**
     * Returns the prefixer's packed length and the interpreter's packed length.
	 * @see org.jpos.iso.ISOFieldPackager#getMaxPackedLength()
	 */
    public int getMaxPackedLength()
    {
        return prefixer.getPackedLength() + interpreter.getPackedLength(getLength());
    }

    /**
	 * Convert the component into a byte[].
	 */
    public byte[] pack(ISOComponent c) throws ISOException
    {
        try
        {
            String paddedData = padder.pad(((String)c.getValue()), getLength());
            byte[] rawData = new byte[prefixer.getPackedLength()
                    + interpreter.getPackedLength(paddedData.length())];
            prefixer.encodeLength(paddedData.length(), rawData);
            interpreter.interpret(paddedData, rawData, prefixer.getPackedLength());
            return rawData;
        } catch(ISOException e)
        {
            Object fieldKey = "unknown";
            if (c != null)
            {
                try
                {
                    fieldKey = c.getKey();
                } catch (Exception ignore)
                {
                }
            }
            throw new ISOException(this.getClass().getName() + ": Problem packing field " + fieldKey, e);
        } catch(Exception e)
        {
            Object fieldKey = "unknown";
            if (c != null)
            {
                try
                {
                    fieldKey = c.getKey();
                } catch (Exception ignore)
                {
                }
            }
            throw new ISOException(this.getClass().getName() + ": Problem packing field " + fieldKey, e);
        }
    }

    /**
     * Unpacks the byte array into the component.
     * @param c The component to unpack into.
     * @param b The byte array to unpack.
     * @param offset The index in the byte array to start unpacking from.
     * @return The number of bytes consumed unpacking the component.
     */
    public int unpack(ISOComponent c, byte[] b, int offset) throws ISOException
    {
        try
        {
            int len = prefixer.decodeLength(b, offset);
            if (len == -1)
            {
                // The prefixer doesn't know how long the field is, so use
    			// maxLength instead
                len = getLength();
            }
            int lenLen = prefixer.getPackedLength();
            String unpacked = interpreter.uninterpret(b, offset + lenLen, len);
            c.setValue(unpacked);
            return lenLen + interpreter.getPackedLength(len);
        } catch(ISOException e)
        {
            Object fieldKey = "unknown";
            if (c != null)
            {
                try
                {
                    fieldKey = c.getKey();
                } catch (Exception ignore)
                {
                }
            }
            throw new ISOException(this.getClass().getName() + ": Problem unpacking field " + fieldKey, e);
        } catch(Exception e)
        {
            Object fieldKey = "unknown";
            if (c != null)
            {
                try
                {
                    fieldKey = c.getKey();
                } catch (Exception ignore)
                {
                }
            }
            throw new ISOException(this.getClass().getName() + ": Problem unpacking field " + fieldKey, e);
        }
    }

    /**
     * Unpack the input stream into the component.
     * @param c  The Component to unpack into.
     * @param in Input stream where the packed bytes come from.
     * @exception IOException Thrown if there's a problem reading the input stream.
     */
    public void unpack (ISOComponent c, InputStream in) 
        throws IOException, ISOException
    {
        int lenLen = prefixer.getPackedLength ();
        int len;
        if (lenLen == 0)
        {
            len = getLength();
        } else
        {
            len = prefixer.decodeLength (readBytes (in, lenLen), 0);
        }
        int packedLen = interpreter.getPackedLength(len);
        String unpacked = interpreter.uninterpret(readBytes (in, packedLen), 0, len);
        c.setValue(unpacked);
    }
}